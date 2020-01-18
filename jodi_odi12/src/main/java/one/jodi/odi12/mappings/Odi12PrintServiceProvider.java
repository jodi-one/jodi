package one.jodi.odi12.mappings;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.service.interfaces.TransformationPrintServiceProvider;
import one.jodi.logging.OdiLogHandler;
import one.jodi.odi12.folder.Odi12FolderServiceProvider;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.adapter.relational.IKey;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.MapAttribute;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.component.FilterComponent;
import oracle.odi.domain.mapping.component.JoinComponent;
import oracle.odi.domain.mapping.component.LookupComponent;
import oracle.odi.domain.mapping.exception.MapComponentException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.expression.MapExpression;
import oracle.odi.domain.mapping.physical.ExecutionUnit;
import oracle.odi.domain.mapping.physical.MapPhysicalDesign;
import oracle.odi.domain.mapping.physical.MapPhysicalNode;
import oracle.odi.mapping.generation.GenerationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Odi12PrintServiceProvider implements TransformationPrintServiceProvider {


    public final static String MAPPING_IN_LIST = "jodi.pbe.include.tables";
    public final static String MAPPING_IGNORE_LIST = "jodi.pbe.ignore.tables";
    private final static Logger logger =
            LogManager.getLogger(Odi12PrintServiceProvider.class);
    private final Odi12FolderServiceProvider folderService;
    private final JodiProperties properties;

    private final ErrorWarningMessageJodi errorWarningMessageJodi;
    private Predicate<MapRootContainer> alwaysMatches = mapping -> true;

    @Inject
    public Odi12PrintServiceProvider(final Odi12FolderServiceProvider folderService,
                                     final JodiProperties properties,
                                     final ErrorWarningMessageJodi errorWarningMessageJodi) {
        this.folderService = folderService;
        this.properties = properties;
        this.errorWarningMessageJodi = errorWarningMessageJodi;
    }

    @SuppressWarnings("unchecked")
    public List<MapRootContainer> findByProjectByFolder(final String folderPath) {
        return (List<MapRootContainer>) (List<?>)
                folderService.findFolderAndSubfolders(folderPath,
                        properties.getProjectCode())
                        .stream()
                        .flatMap(f -> f.getMappings()
                                .stream())
                        .sorted((m1, m2) -> m1.getName().compareTo(m2.getName()))
                        .collect(Collectors.toList());
    }

    private boolean hasPostfixOf(final MapRootContainer mapping,
                                 final Set<String> postfixes) {
        return postfixes.stream()
                .filter(p -> mapping.getName().endsWith(p))
                .findFirst()
                .isPresent();
    }

    private boolean hasPrefixOf(MapRootContainer mappingName, String prefix) {
        return prefix == null ? true : mappingName.getName().startsWith(prefix);
    }

    @Override
    public void print(String folderName, String prefix) {
        logger.info("Tip: set log format to:");
        logger.info("log4j.appender.warnfile.layout.ConversionPattern=%m%n");
        logger.info("Joins are converted to uppercase.");
        logger.info("_[0-9]A is replaced with CURRENT");
        logger.info("_[0-9] is replaced with EFF_DAT");

        final Set<String> mappingNames = new HashSet<>();
        if (this.properties.getPropertyKeys().contains(MAPPING_IN_LIST)) {
            mappingNames.addAll(this.properties.getPropertyList(MAPPING_IN_LIST));
        }

        if (this.properties.getPropertyKeys().contains(MAPPING_IGNORE_LIST)) {
            for (String ignore : this.properties.getPropertyList(MAPPING_IGNORE_LIST)) {
                Iterator<String> iter = mappingNames.iterator();
                while (iter.hasNext()) {
                    String name = iter.next();
                    if (name.endsWith(ignore)) {
                        iter.remove();
                    }
                }
            }
        }

        Predicate<MapRootContainer> mappingFilter = mappingName -> hasPrefixOf(mappingName, prefix);
        if (mappingNames.size() > 0) {
            mappingFilter = mappingName -> hasPostfixOf(mappingName, mappingNames) && hasPrefixOf(mappingName, prefix);
        }
        findByProjectByFolder(folderName).stream()
                .filter(mappingFilter)
                .sorted((MapRootContainer o1, MapRootContainer o2) -> subStringMap(o1.getName()).compareTo(subStringMap(o2.getName())))
                .forEach(m1 -> {
                    try {
                        print(m1);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                });
    }

    private String subStringMap(String name) {
        int index = name.indexOf("W_X");
        int index2 = name.indexOf("Y_");
        if (index > -1)
            return name.substring(index, name.length());
        else if (index2 > -1)
            return name.substring(index2, name.length());
        else
            return name;
    }

    private void print(MapRootContainer m) throws Exception {
        try {
            List<? extends IMapComponent> allComponents = m.getAllComponents();
            @SuppressWarnings("unchecked")
            List<JoinComponent> joins =
                    (List<JoinComponent>) allComponents.stream()
                            .filter(c -> c instanceof JoinComponent)
                            .collect(Collectors.toList());
            try {
                Collections.sort(joins, (m1, m2) -> {
                    String secondName = "";
                    {
                        List<? extends IMapComponent> joined = null;
                        try {
                            joined =
                                    m1.getReferencedSourcesFromJoinCondition(m.getAllComponents());
                        } catch (AdapterException | MappingException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                        String firstName =
                                joined.get(0).getAlias() != null ? joined.get(0).getAlias()
                                        : joined.get(0).getName();
                        secondName =
                                joined.get(1).getAlias() != null ? joined.get(1).getAlias()
                                        : joined.get(1).getName();
                        try {
                            if (secondName.equals(m.getSources().get(0).getName())) {
                                secondName = firstName;
                            }
                        } catch (MapComponentException e) {
                            logger.error(e);
                        }
                    }
                    String secondName2 = "";
                    {
                        List<? extends IMapComponent> joined2 = null;
                        try {
                            joined2 =
                                    m2.getReferencedSourcesFromJoinCondition(m.getAllComponents());
                        } catch (AdapterException | MappingException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                        String firstName2 =
                                joined2.get(0).getAlias() != null ? joined2.get(0).getAlias()
                                        : joined2.get(0).getName();
                        secondName2 =
                                joined2.get(1).getAlias() != null ? joined2.get(1).getAlias()
                                        : joined2.get(1).getName();
                        try {
                            if (secondName2.equals(m.getSources().get(0).getName())) {
                                secondName2 = firstName2;
                            }
                        } catch (MapComponentException e) {
                            logger.error(e);
                        }
                    }

                    return secondName.compareTo(secondName2);
                });
            } catch (IndexOutOfBoundsException ioobe) {
                logger.info("Only 1 join in source: " + m.getName());
            }
            for (IMapComponent comp : allComponents) {
                if (comp instanceof FilterComponent) {
                    logger.info(String.format("Filter '%s'.",
                            ((FilterComponent) comp).getFilterCondition()
                                    .getText().trim()));
                }
            }
            for (IMapComponent comp : joins) {
                if (comp instanceof LookupComponent) {
                    LookupComponent lc = ((LookupComponent) comp);
                    List<IMapComponent> joined =
                            lc.getReferencedSourcesFromJoinCondition(m.getAllComponents());
                    String firstName =
                            joined.get(0).getAlias() != null ? joined.get(0).getAlias()
                                    : joined.get(0).getName();
                    String join = printJoin(lc.getJoinCondition());
                    if (joined.size() > 1) {
                        String secondName =
                                joined.get(1).getAlias() != null ? joined.get(1).getAlias()
                                        : joined.get(1).getName();
                        logger.info(String.format("Join '%s' and '%s' with join: '%s'.",
                                replace1and1A(firstName.trim()),
                                replace1and1A(secondName.trim()), join));
                    } else {
                        logger.info(String.format("Join '%s' and '%s' with join: '%s'.",
                                replace1and1A(firstName.trim()), "unknown",
                                join));
                    }
                } else if (comp instanceof JoinComponent) {
                    JoinComponent jc = ((JoinComponent) comp);
                    List<IMapComponent> joined =
                            jc.getReferencedSourcesFromJoinCondition(m.getAllComponents());
                    String firstName =
                            joined.get(0).getAlias() != null ? joined.get(0).getAlias()
                                    : joined.get(0).getName();
                    String join = printJoin(jc.getJoinCondition());
                    if (joined.size() > 1) {
                        String secondName =
                                joined.get(1).getAlias() != null ? joined.get(1).getAlias()
                                        : joined.get(1).getName();
                        logger.info(String.format("Join '%s' and '%s' with join: '%s'.",
                                replace1and1A(firstName.trim()),
                                replace1and1A(secondName.trim()), join));
                    } else {
                        logger.info(String.format("Join '%s' and '%s' with join: '%s'.",
                                replace1and1A(firstName.trim()), "", join));
                    }
                }
            }
        } catch (AdapterException | MappingException e) {
            String message = "Exception while printing.";
            logger.info(message, e);
            throw new RuntimeException(message, e);
        }
        if (containsAggregateComponent(m)) {
            for (IMapComponent t : m.getAllComponents()) {
                if (t.getTypeName().equals("AGGREGATE")) {
                    for (MapAttribute a : t.getAttributes()) {
                        printAggregate(m, a);
                    }
                }
            }
        } else {
            try {
                for (IMapComponent t : m.getTargets()) {
                    Set<String> updateKeyColumns = new HashSet<>();
                    if (t instanceof DatastoreComponent) {
                        IKey updateKey = ((DatastoreComponent) t).getUpdateKey();
                        if (updateKey != null) {
                            updateKeyColumns = updateKey.getColumns()
                                    .stream()
                                    .map(c -> c.getName())
                                    .collect(Collectors.toSet());
                        }
                    }

                    for (MapAttribute a : t.getAttributes()) {
                        boolean isUpdateKey = updateKeyColumns.contains(a.getName());
                        printTargetDataStore(m, a, isUpdateKey);
                    }
                }
            } catch (MappingException e) {
                logger.error(e);
            }
        }
        printKMs(m);
    }

    private String printJoin(MapExpression joinCondition) {
        TreeMap<String, String> sorted = new TreeMap<String, String>();
        List<String> piecesSplitByAnd = splitToAnd(joinCondition.getText());
        piecesSplitByAnd.stream().forEach(s -> sorted.put(replace1and1A(s.toUpperCase()),
                replace1and1A(s.toUpperCase())));
        StringBuilder sb = new StringBuilder();
        sorted.values().forEach(s -> sb.append(s + " AND "));
        return replace1and1A(sb.toString().substring(0, (sb.toString().length()) - 5));
    }

    private String replace1and1A(String piece) {
//      return piece;
        String cleaned1 = piece.replaceAll("(_){1,1}[0-9]{1,1}(A){1,1}", "_CURRENT");
        String cleaned2 = cleaned1.replaceAll("(_){1,1}[0-9]{1,1}", "_EFF_DAT");
        return cleaned2;
    }

    private List<String> splitToAnd(String text) {
        List<String> retValue = new ArrayList<String>();
        String[] words = text.split(" ");
        String handle = "";
        boolean isBetween = false;
        for (String word : words) {

            if (word.equalsIgnoreCase("between") && !isBetween) {
                isBetween = true;
            }
            if ((word.equalsIgnoreCase("and") || word.equalsIgnoreCase("or")) &&
                    !isBetween) {
                handle = handle.trim();
                if (handle.toLowerCase().startsWith("and "))
                    handle = handle.substring("and ".length(), handle.length());
                handle = handle.trim();
                if (handle.toLowerCase().startsWith("or "))
                    handle = handle.substring("or ".length(), handle.length());
                handle = handle.trim();
                retValue.add(handle);
                handle = "";
            }
            handle += " " + word.replace("D1", "").trim();
            if (isBetween && (word.equalsIgnoreCase("and") ||
                    word.equalsIgnoreCase("and"))) {
                isBetween = false;
            }
        }
        handle = handle.trim();
        if (handle.toLowerCase().startsWith("and "))
            handle = handle.substring("and ".length(), handle.length());
        handle = handle.trim();
        if (handle.toLowerCase().startsWith("or "))
            handle = handle.substring("or ".length(), handle.length());
        handle = handle.trim();
        retValue.add(handle);
        return retValue;
    }

    private void printTargetDataStore(MapRootContainer m, MapAttribute a, boolean isUpdateKey) {
        // String message;
        // try {
        // message = m.getName() + ":" + a.getName() + " insert '" +
        // a.isInsertIndicator() + "' update '" +
        // a.isUpdateIndicator() + "' active '" + a.isActive() +
        // "' check not null '" + a.isCheckNotNullIndicator() +
        // "' key '" + isUpdateKey + "' exec '" +
        // a.getExecuteOnHint() + "'.";
        // logger.info(message);
        List<MapExpression> expressions = a.getExpressions();
        expressions.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        expressions.stream()
                .filter(e -> e.getText() != null &&
                        !e.getText().equalsIgnoreCase("null"))
                .forEach(e -> logger.info(cleanMappingName(m) + ":" +
                        a.getName() + " expression '" +
                        replace1and1A(e.getText()) + "'"));
        // } catch (MappingException e1) {
        // logger.error(e1);
        // }
    }

    private void printAggregate(MapRootContainer m, MapAttribute a) {
        List<MapExpression> expressions = a.getExpressions();
        expressions.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        expressions.stream()
                .filter(e -> e.getText() != null &&
                        !e.getText().equalsIgnoreCase("null"))
                .forEach(e -> logger.info(cleanMappingName(m) + ":" +
                        a.getName() + " expression '" +
                        replace1and1A(e.getText()) +
                        "'"));
    }

    private boolean containsAggregateComponent(MapRootContainer m) {
        try {
            return m.getAllComponents()
                    .stream()
                    .filter(comp -> comp.getTypeName().equals("AGGREGATE"))
                    .count() > 0;
        } catch (MapComponentException e) {
            return false;
        }
    }

    private String cleanMappingName(MapRootContainer mapping) {
        String folder = mapping.getFolder().getQualifiedName();
        if (mapping.getName().contains("_W_X_"))
            return folder + "/" + mapping.getName().substring(mapping.getName().indexOf("_W_X_") + 1, mapping.getName().length());
        else if (mapping.getName().contains("Y_"))
            return folder + "/" + mapping.getName().substring(mapping.getName().indexOf("Y_"), mapping.getName().length());
        else
            return folder + "/" + mapping.getName();
    }


    private void printKMs(MapRootContainer m) {
        // suppress ODI logger messages when generating scenarios
        java.util.logging.Logger odiLogger =
                java.util.logging.Logger.getLogger("oracle.odi.mapping");
        odiLogger.setLevel(Level.WARNING);
        OdiLogHandler odiHandler = new OdiLogHandler(errorWarningMessageJodi);
        odiHandler.setLevel(Level.WARNING);
        odiLogger.addHandler(odiHandler);
        printIKM(m);
        printCKM(m);
        printLKM(m);
    }

    private void printLKM(MapRootContainer m) {
        if (!(m instanceof Mapping)) {
            return;
        }
        for (MapPhysicalDesign physicalDesign : ((Mapping) m).getExistingPhysicalDesigns()) {
            for (MapPhysicalNode node : physicalDesign.getAllAPNodes()) {
                //
                logger.info(subStringMap(m.getName()) + " LKM " + node.getLKM().getName());
                try {
                    node.getLKMOptionValues().stream()
                            .forEach(o -> logger.info(o.getName() + " : " + o.getOptionValueString()));
                } catch (AdapterException | MappingException e) {
                    logger.error(e);
                }
            }
        }
    }

    private void printCKM(MapRootContainer m) {
        if (m instanceof Mapping) {
            for (MapPhysicalDesign pd : ((Mapping) m).getExistingPhysicalDesigns()) {
                try {
                    for (ExecutionUnit teu : pd.getTargetExecutionUnits()) {
                        for (MapPhysicalNode target : teu.getTargetNodes()) {
                            logger.info(subStringMap(m.getName()) + " CKM " + target.getCheckKM().getName());
                            try {
                                target.getCheckKMOptionValues()
                                        .forEach(o -> logger.info(o.getName() + " " + o.getOptionValueString()));
                            } catch (AdapterException | MappingException | GenerationException e) {
                                logger.error(e);

                            }
                        }
                    }
                } catch (AdapterException | MappingException e) {
                    logger.error(e);

                }
            }
        }
    }

    private void printIKM(MapRootContainer m) {
        for (MapPhysicalDesign pd : ((Mapping) m).getExistingPhysicalDesigns()) {
            // here are the target IKMs set
            try {
                for (ExecutionUnit teu : pd.getTargetExecutionUnits()) {
                    for (MapPhysicalNode target : teu.getTargetNodes()) {
                        assert (target != null);
                        logger.info(subStringMap(m.getName()) + " IKM " + target.getIKM().getName());
                        try {
                            target.getIKMOptionValues().stream().filter(o -> !o.getName().equalsIgnoreCase("OPTIMIZER_HINT"))
                                    .forEach(o -> logger.info(o.getName() + " " + o.getOptionValueString()));
                        } catch (AdapterException | MappingException | GenerationException e) {
                            logger.error(e);
                        }
                    }
                }
            } catch (AdapterException | MappingException e) {
                logger.error(e);
            }
        }
    }
}
