import java.text.SimpleDateFormat

/**
 * @author dvleeuwe \n
 * @since 10/20/20 \n
 *
 * This script basically passed command line arguments to all schemas,
 * and executes it on all schemas defined in schemas.
 *
 * It uses the LB_URL for connection details.
 *
 * The script requires the SQLCL binary from Oracle to be added to the path.
 * <a href="https://www.oracle.com/tools/downloads/sqlcl-downloads.html">SQLCL</a>
 *
 * Please note
 * 1) if label is used in genschema it complains that "label" is not allowed to appear in element
 *
 * Usage:
 * # initial generation, the -debug is required otherwise INF won't generate.
 * # also check for left over database tables starting with database*
 * # the tables are not dropped if something fails, which prevent the next run of lb.
 * 1) check no leftovers previous release select * from dba_tables where table_name like 'DATABASE%' ;
 * 2) groovy liquibase.groovy genschema -context LEGACY -debug > genschema_log_LEGACY.txt
 * # validate initial generation
 * 3) groovy liquibase.groovy validate -changelog controller.xml > validation_log.tx
 * # to install in new cloud (OCI) environment create legacy.sql
 * 4a) groovy liquibase.groovy updatesql -changelog controller.xml -context LEGACY -noreport > LEGACY.sql
 * # errors noticed: see legacy_gamma6.sql.log
 *
 ORA-02264: name already used by an existing constraint
 ORA-02449: unique/primary keys in table referenced by foreign keys
 ORA-06564: object NM_ENV does not exist
 * #
 *
 * # to install in new cloud env execute in SQLCL change URL!!!!
 * 4b) sql /nolog @legacy.sql see legacy_gamma6.sql.log
 * # mark release as installed
 * 5) groovy liquibase.groovy changelogsync -changelog controller.xml > legacy_changelogsync.log
 * # install post_legacy -> dependencies on dependencies such as GDPRS from EXN15 -> EDW
 * add to each controller.xml the output of genschema_log.txt <include file> - entries
 * validate post legacy install
 * 6) groovy liquibase.groovy validate -changelog controller.xml > validation_log.txt
 * mark post legacy install as installed
 * ---------------------------------------------------------------------------------------------------------------------
 * -- steps before this line don't need to be repeated once they are done, steps after this line need to be repeated,
 * -- once development for current release is done
 * ---------------------------------------------------------------------------------------------------------------------
 * 7) groovy liquibase.groovy changelogsync -changelog controller.xml
 * Start developing
 * 8) groovy liquibase.groovy genschema -context testrelease
 * # create script to put on TST / ACC / PRD environment
 * 9) groovy liquibase.groovy updatesql -changelog controller.xml -noreport > update.sql
 * # execute script on TST / ACC change URL!!!!!!
 * 10) sql /nolog @update.sql
 * # execute in PRD change URL!!!!!!
 * 11) sql /nolog @update.sql
 * # mark release as installed request new clone
 * 12) groovy liquibase.groovy changelogsync -changelog controller.xml
 */

class LiquibaseRunner{
    File resource = new File("./src/main/resources/liquibase")

    static void main(String[] args) {
        def lbCommand =""
        def LB_URL = System.getProperty("LB_URL")
        def LB_CLOUDCONFIG = System.getProperty("LB_CLOUDCONFIG")
        def LB_DB_PWD = System.getProperty("LB_DB_PWD")

        for(String arg : args){
            lbCommand += " " +arg
        }
        def firstCommand = args[0]

        assert firstCommand
        assert LB_URL
        assert LB_CLOUDCONFIG
        assert LB_DB_PWD

        new LiquibaseRunner().processLb(lbCommand, firstCommand, LB_URL, LB_DB_PWD, LB_CLOUDCONFIG)
    }

    def schemas = [
            "ODITMP","EXN1","EXN2","EXN6","EXN15", "EXN23","EXN12", "EXN10","EXN11","EXN13","EXN14","EXN16","EXN17","EXN18","EXN19","EXN20",
            "EXN21","EXN22","EXN24","EXN25","EXN26","EXN27","EXN28","EXN29","EXN3","EXN30","EXN4","EXN5",
            "EXN7","EXN8","EXN9","MCM","MTM","STG1","STG10","STG11","STG12","STG13","STG14",
            "STG15","STG16","STG17","STG18","STG19","STG2","STG20","STG21","STG22","STG23","STG24","STG25","STG26",
            "STG27","STG28","STG29","STG3","STG30","STG4","STG5","STG6","STG7","STG8","STG9", "EDW",
            "ERM",
            "INF" //TODO find out why this fails, INF doesn't fail if -debug flag is set and
            // when no left over DATABASE tables present
    ]

    def processLb(lbCommand, firstCommand, LB_URL, LB_DB_PWD, LB_CLOUDCONFIG){




        schemas.each{
            schema ->



                def logFile = new File(resource, firstCommand+"_"+schema +"_log.txt")
                logFile.write("")
                def date = new Date()
                def sdf = new SimpleDateFormat("yyyy/MM/dd/H:m:s")
                writeLogLine ("--------------------------------------------------------------------------------------------------------------", logFile)
                writeLogLine ("-- Generated with LiquibaseRunner.groovy at "+sdf.format(date), logFile)
                writeLogLine ("-- groovy LiquibaseRunner.groovy $lbCommand", logFile)
                writeLogLine ("-- logFile written to $logFile.absolutePath", logFile)
                writeLogLine ("-- LB_URL $LB_URL --this is standard connection", logFile)
                writeLogLine ("--------------------------------------------------------------------------------------------------------------", logFile)

                def tablespaceOdiTMP= "ODITMP"

                File schemaDir = new File(resource, schema)
                schemaDir.mkdirs()
                if(lbCommand.toString().contains("diff")){
                    lbCommand +=  """  -user $schema """
                }

                File cmdFile= new File(resource, "cmd.sql")
                cmdFile.write("set ddl storage off\n" +
                        "set ddl tablespace off\n")
                cmdFile.append("execute odi_grants();\n")
                cmdFile.append("""lb $lbCommand\n""".toString())
                cmdFile << "exit\n"
                assert LB_DB_PWD
                def cloudConfig = LB_CLOUDCONFIG != null  && LB_CLOUDCONFIG.toString().length() > 2 ? """ -cloudconfig $LB_CLOUDCONFIG """ : ""
                def cmd = "";
//                if(lbCommand.toString().toLowerCase().contains(" update ")){
//                    cmd = """cd $schemaDir.absolutePath;\nsql $cloudConfig -S ODITMP/'$LB_DB_PWD'@$LB_URL @$cmdFile.absolutePath\n"""
//                }else{
                cmd =  """cd $schemaDir.absolutePath;\nsql $cloudConfig -S $schema/'$LB_DB_PWD'@$LB_URL @$cmdFile.absolutePath\n"""
//                }
                File cmdBashFile = new File(resource,"cmd.sh")
                cmdBashFile.write("#!/bin/sh\n")
                cmdBashFile << cmd.toString()

                executeLb("""sh $cmdBashFile.absolutePath""", schemaDir, schema, cmdFile, cmd, logFile)
                //somehow liquibase doesn't commit.
                writeLogLine("commit;", logFile)
                cleanupController("""$schemaDir.absolutePath/controller.xml""")
        }
    }

    void cleanupController(file){
        File clean = new File(file+".new");
        clean.write("")
        new File(file).eachLine { line ->
            if(!line.contains("\$") && !line.contains("_bad_")) {
                clean << line +"\n"
            }
        }
        new File(file).delete();
        clean.renameTo(new File(file));
    }


    def executeLb(bashCmd, schemaDir, schema, cmdFile, cmd, logFile){
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = bashCmd.toString().execute()
        proc.consumeProcessOutput(sout, serr)
        proc.waitForOrKill(1000 * 60 * 60)
        //println "out> $sout err> $serr"
        checkErrors(serr, logFile)
        handleOutput(sout, schemaDir, schema, cmdFile, cmd, logFile)
    }

    def checkErrors(serr, logFile){
        Collection<String> lines = serr.toString().split("\\n")
        Optional<String> firstError = lines.stream().filter{ l -> (!l.toString().startsWith("[INFO]")
                && l.toString().size() > 4 )}.findFirst()
        if(firstError.isPresent()){
            println("--[ERROR][ORA-] Found errors!")
            lines.stream().each { erLine -> println("--[ERROR]"+erLine)}
            System.exit(1)
        }
    }

    def handleOutput(sout, schemaDir, schema, cmdFile, cmd, logFile) {
        Collection<String> lines = sout.toString().split("\\n")
        lines.stream().each { it ->
            if (it.toString().startsWith("LoggingExecutor")
                    || it.toString().startsWith("Operation is successfully completed")
                    || it.toString().startsWith("Action successfully completed")
                    || it.toString().startsWith("######## ERROR SUMMARY ##################")
                    || it.toString().startsWith("Errors encountered:0")
                    || it.toString().startsWith("Using temp directory:")
                    || it.toString().startsWith("ScriptRunner Executing:")
                    || it.toString().startsWith("ScriptRunner")
            ) {
                writeLogLine("-- " + it, logFile)
            } else if (it.toString().startsWith("Processing has failed")) {
                writeLogLine("--[ERROR] " + it, logFile)
                /// System.exit(1)
            } else if (it.toString().contains("_bad_") && it.toString().trim().endsWith(".xml")) {
                // bad objects are materialized views or other views,
                // that are not generated appropiately and need to be used with genobject
                //TODO MVs go manual.
                //handleBadObjects(it, schemaDir, schema, cmdFile, cmd, logFile)
            } else if (it.toString().contains("DELETE FROM ${schema}.DATABASECHANGELOGLOCK;")) {
//                writeLogLine("grant delete,insert, update on ${schema}.DATABASECHANGELOG to oditmp;", logFile)
//                writeLogLine("grant delete,insert, update on ${schema}.DATABASECHANGELOG_ACTIONS to oditmp;", logFile)
//                writeLogLine("grant delete,insert, update on ${schema}.DATABASECHANGELOGLOCK to oditmp;", logFile)
                writeLogLine(it.toString(), logFile)
            } else if (it.toString().startsWith("CREATE MATERIALIZED VIEW")) {
                writeLogLine(it.toString().replace("CREATE", "DROP").substring(0, it.indexOf("(") - 2) + ";\n", logFile)
                writeLogLine(it.toString(), logFile)
            } else if (it.toString().startsWith("END;")) {
                writeLogLine(it.toString(), logFile)
                writeLogLine("/", logFile)
            }else{
                writeLogLine(it.toString(), logFile)
            }
        }
    }

    def handleBadObjects(xmlFileName, schemaDir, schema, cmdFile, cmd, logFile){
        File badFile= new File(schemaDir, xmlFileName.trim())
        def buildType =""
        def name = ""
        for(String line : badFile.text.split("\\n")){
            if(line.trim().startsWith("<BUILD>")){
                buildType = line.replace("<BUILD>","").replace("</BUILD>","").trim()
            }
            if(line.trim().startsWith("<NAME>")){
                name = line.replace("<NAME>","").replace("</NAME>","").trim()
            }
        }
        String fileContent = badFile.text.replace("<BUILD>IMMEDIATE</BUILD>","<BUILD>DEFERRED</BUILD>")
        badFile.write(fileContent)
        writeLogLine("<!-- <include file=\"${xmlFileName}\"/> --><!-- [ERROR_NEEDS_SORTING][$schema][$name][Build is now deferred was $buildType]" + xmlFileName  + " add this to $schemaDir.absolutePath/controller.xml -->", logFile)
    }

    def writeLogLine(content, logFile){
        logFile << """$content\n"""
        println(content)
    }

    def removeGeoSpatial(File controller){
        StringBuilder fixed = new StringBuilder()
        controller.withReader('UTF-8') { reader ->
            def line
            while ((line = reader.readLine()) != null) {
                if(line.contains("<include file") && line.contains("\$")){
                    // skipping geospatial stuff
                }else{
                    fixed.append(line +"\n");
                }
            }
        }
        controller.write("")
        controller << fixed
    }
}