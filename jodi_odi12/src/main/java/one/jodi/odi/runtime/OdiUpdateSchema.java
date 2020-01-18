package one.jodi.odi.runtime;

import oracle.odi.domain.topology.OdiDataServer;
import oracle.odi.domain.topology.OdiPhysicalSchema;
import oracle.odi.domain.topology.finder.IOdiDataServerFinder;
import oracle.odi.domain.topology.finder.IOdiPhysicalSchemaFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class OdiUpdateSchema {

    private final static Logger logger = LogManager.getLogger(OdiUpdateSchema.class);

    public static void main(String[] args) {
        OdiUpdateSchema odiUpdateSchema = new OdiUpdateSchema();
        if (args.length != 12) {
            logger.info(odiUpdateSchema.getUsage().toString());
            return;
        }
        String odiMasterRepoUrl = args[0].trim();
        String odiMasterRepoUser = args[1].trim();
        String odiMasterRepoPassword = args[2].trim();
        String odiWorkRepo = args[3].trim();
        String odiLoginUsername = args[4].trim();
        String odiLoginPassword = args[5].trim();
        String pDataServerName = args[6].trim();
        String pUsername = args[7].trim();
        String pPassword = args[8].trim();
        String pServerInstanceName = args[9].trim();
        String pJdbcDriverNameRepository = args[10].trim();
        String pSchemaName = args[11].trim();
        odiUpdateSchema.updateSchema(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword, odiWorkRepo,
                odiLoginUsername, odiLoginPassword, pDataServerName, pUsername, pPassword,
                pServerInstanceName, pJdbcDriverNameRepository, pSchemaName);
    }

    public void updateSchema(String odiMasterRepoUrl, String odiMasterRepoUser, String odiMasterRepoPassword,
                             String odiWorkRepo, String odiLoginUsername, String odiLoginPassword, String dataServerName,
                             String username, String password, String serverInstanceName,
                             String jdbcDriverRepository, String schemaName) {
        // Create a Data Server , Physical Schema, Logical Schema
        OdiConnection odiConnection = OdiConnectionFactory.getOdiConnection(odiMasterRepoUrl, odiMasterRepoUser, odiMasterRepoPassword,
                odiLoginUsername, odiLoginPassword, jdbcDriverRepository, odiWorkRepo);
        OdiDataServer oracleDataServer = ((IOdiDataServerFinder) odiConnection.getOdiInstance()
                .getTransactionalEntityManager().getFinder(OdiDataServer.class)).findByName(dataServerName);
        IOdiPhysicalSchemaFinder finder = ((IOdiPhysicalSchemaFinder) odiConnection.getOdiInstance()
                .getTransactionalEntityManager().getFinder(OdiPhysicalSchema.class));
        Collection<OdiPhysicalSchema> schemas = oracleDataServer.getPhysicalSchemas();
        OdiPhysicalSchema attachedSchema = null;
        if (schemas.size() == 1) {
            OdiPhysicalSchema schema = schemas.iterator().next();
            attachedSchema = (OdiPhysicalSchema) finder.findById(schema.getInternalId());
            attachedSchema.setSchemaName(schemaName);
        } else {
            throw new RuntimeException("More than one or 0 schemas found.");
        }
        odiConnection.getOdiInstance().getTransactionalEntityManager().persist(attachedSchema);
        odiConnection.getOdiInstance().getTransactionManager().commit(odiConnection.getTransactionStatus());
        odiConnection.getOdiInstance().close();
    }

    public StringBuffer getUsage() {
        StringBuffer usage = new StringBuffer("Usage:\n");
        usage.append(
                "one.jodi.odi.OdiUpdateSchema odiMasterRepoUrl odiMasterRepoUser odiMasterRepoPassword odiWorkRepo odiLoginUsername odiLoginPassword pDataServerName pUsername pPassword pServerInstanceName jdbcDriverRepository  schemaname\n");
        usage.append("java -Dlog4j.configuration=./conf/log4j.properties -classpath ./lib/*:$ODI_LIB_PATH one.jodi.odi.runtime.OdiUpdateSchema jdbc:oracle:thin:@jodi:1521/samplec DEV_ODI_REPO ODI_MASTER_REPO_PASS WORKREP SUPERVISOR ODI_USER_PASS ORACLE_CHINOOK_DEV CINOOKT NEW_DB_PASS NEW_TNSNAME oracle.jdbc.driver.OracleDriver  SYS\n");
        usage.append("\n");
        return usage;
    }

}
