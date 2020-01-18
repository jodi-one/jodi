package one.jodi.odi12.sequences;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.VariableServiceException;
import one.jodi.etl.internalmodel.NativeSequence;
import one.jodi.etl.internalmodel.Sequence;
import one.jodi.etl.internalmodel.SpecificSequence;
import one.jodi.etl.internalmodel.impl.NativeSequenceImpl;
import one.jodi.etl.internalmodel.impl.SpecificSequenceImpl;
import one.jodi.etl.internalmodel.impl.StandardSequenceImpl;
import one.jodi.odi.sequences.OdiSequenceAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.OdiSequence;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.project.finder.IOdiSequenceFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class Odi12SequenceAccessStrategy implements OdiSequenceAccessStrategy {

    private final OdiInstance odiInstance;
    private final String projectCode;
    private final Logger logger = LogManager.getLogger(Odi12SequenceAccessStrategy.class);

    @Inject
    public Odi12SequenceAccessStrategy(final OdiInstance odiInstance,
                                       final JodiProperties jodiProperties) {
        this.odiInstance = odiInstance;
        this.projectCode = jodiProperties.getProjectCode();
    }

    @Override
    public Collection<OdiSequence> findAll() {
        IOdiSequenceFinder finder =
                ((IOdiSequenceFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiSequence.class));
        Collection<OdiSequence> all = finder.findByProject(this.projectCode);
        return all;
    }

    @Override
    public Collection<OdiSequence> findAllGlobals() {
        IOdiSequenceFinder finder =
                ((IOdiSequenceFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiSequence.class));
        Collection<OdiSequence> all = finder.findAllGlobals();
        return all;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void create(Sequence internalSequence) {
        OdiProject project =
                internalSequence.getGlobal() ? null : findProject(this.projectCode);

        boolean exists = sequenceExists(project, internalSequence.getName());
        OdiSequence odiSequence = findOrCreateSequence(project, internalSequence.getName(),
                internalSequence.getIncrement());
        if (internalSequence instanceof StandardSequenceImpl) {


            odiSequence.setType(OdiSequence.SequenceType.STANDARD);
        } else if (internalSequence instanceof SpecificSequenceImpl) {
            odiSequence.setType(OdiSequence.SequenceType.SPECIFIC);
            odiSequence.setLogicalSchemaName(((SpecificSequence) internalSequence).getSchema());
            odiSequence.setTableName(((SpecificSequence) internalSequence).getTable());
            odiSequence.setColumnName(((SpecificSequence) internalSequence).getColumn());
            odiSequence.setRowFilter(((SpecificSequence) internalSequence).getFilter());
        } else if (internalSequence instanceof NativeSequenceImpl) {
            odiSequence.setType(OdiSequence.SequenceType.NATIVE);
            odiSequence.setLogicalSchemaName(((NativeSequence) internalSequence).getSchema());
            odiSequence.setNativeSequenceName(((NativeSequence) internalSequence).getNativeName());
        } else {
            throw new VariableServiceException("Can't determine datatype for " +
                    "variable; " + internalSequence.getName());
        }
        if (exists) {
            odiInstance.getTransactionalEntityManager().merge(odiSequence);
        } else {
            odiInstance.getTransactionalEntityManager().persist(odiSequence);
        }
        logger.info("Created sequence: " + internalSequence.getName() + ".");
    }

    private OdiSequence findOrCreateSequence(OdiProject project, String name, Integer increment) {
        boolean exist = sequenceExists(project, name);
        if (!exist) {
            return new OdiSequence(project, name,
                    increment);
        } else {
            IOdiSequenceFinder finder =
                    ((IOdiSequenceFinder) odiInstance.getTransactionalEntityManager()
                            .getFinder(OdiSequence.class));
            OdiSequence retValue = finder.findByName(name, this.projectCode);
            if (retValue != null) {
                return retValue;
            } else {
                return finder.findGlobalByName(name);
            }
        }
    }

    private boolean sequenceExists(OdiProject project, String name) {
        IOdiSequenceFinder finder = ((IOdiSequenceFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiSequence.class));
        if (project == null) {
            return finder.findGlobalByName(name) != null;
        } else {
            OdiSequence retValue = finder.findByName(name, project.getCode());
            return retValue != null;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void delete(Sequence sequence) {
        IOdiSequenceFinder finder =
                (IOdiSequenceFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiSequence.class);
        final OdiSequence var;
        if (sequence.getGlobal()) {
            var = finder.findGlobalByName(sequence.getName());
        } else {
            var = finder.findByName(sequence.getName(), this.projectCode);
        }
        if (var == null) {
            return;
        }
        odiInstance.getTransactionalEntityManager().remove(var);
        logger.info("Deleted sequence: " + sequence.getName() + ".");
    }


    @Cached
    protected OdiProject findProject(String projectCode) {
        IOdiProjectFinder finder =
                (IOdiProjectFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiProject.class);
        OdiProject project = finder.findByCode(projectCode);
        assert (project != null);
        return project;
    }

}
