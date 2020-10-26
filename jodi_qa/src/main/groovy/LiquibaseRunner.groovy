
import java.text.SimpleDateFormat

/**
 * @author dvleeuwe \n
 * @since 10/20/20 \n
 *
 * This script basically passed command line arguments to all schemas,
 * and executes it on all schemas defined in schemas.
 *
 * It uses the urlDev for connection details.
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
        for(String arg : args){
            lbCommand += " " +arg
        }
        def firstCommand = args[0]
        println("$lbCommand")
        new LiquibaseRunner().processLb(lbCommand, firstCommand)
    }

    def schemas = [
            "CHINOOK",
            "DWH_CON_CHINOOK","DWH_CON","DWH_DMT", "DWH_SRC","DWH_STG", "DWH_STI","DWH_STO"
    ]

    def processLb(lbCommand, firstCommand){
        def urlDev = 'DB202007280549_high'
        def urlPrd = 'localhost:1521/ORCL' // over time this should be alpha6
        def logFile = new File(resource, firstCommand + "_log.txt")
        logFile.write("")
        def date = new Date()
        def sdf = new SimpleDateFormat("yyyy/MM/dd/H:m:s")
        writeLogLine ("--------------------------------------------------------------------------------------------------------------", logFile)
        writeLogLine ("-- Generated with LiquibaseRunner.groovy at "+sdf.format(date), logFile)
        writeLogLine ("-- groovy LiquibaseRunner.groovy $lbCommand", logFile)
        writeLogLine ("-- logFile written to $logFile.absolutePath", logFile)
        writeLogLine ("-- urlDev $urlDev --this is standard connection", logFile)
        writeLogLine ("-- urlPrd $urlPrd --this is used for diff as -url param", logFile)
        writeLogLine ("--------------------------------------------------------------------------------------------------------------", logFile)

        schemas.each{
            schema ->
                File schemaDir = new File(resource, schema)
                schemaDir.mkdirs()
                File cmdFile= new File(resource, "cmd.sql")
                def DB_PWD = System.getProperty("OCI_JODI_PWD")
                if(lbCommand.toString().contains("diff")){
                    lbCommand +=  """ -url $urlPrd -user $schema -password $DB_PWD -noreport"""
                }
                cmdFile.write("""lb $lbCommand\n""".toString())
                cmdFile << "exit\n"
                assert DB_PWD
                def cmd = """cd $schemaDir.absolutePath;\nsql -cloudconfig /opt/git/opc/src/main/resources/wallet/Wallet_DB202007280549.zip -S $schema/'$DB_PWD'@$urlDev @$cmdFile.absolutePath\n"""
                File cmdBashFile = new File(resource,"cmd.sh")
                cmdBashFile.write("#!/bin/sh\n")
                cmdBashFile << cmd.toString()
                writeLogLine("""\n\nconnect $schema/LETMEIN@localhost:1521/ORCL\n\n""", logFile)
                executeLb("""sh $cmdBashFile.absolutePath""", schemaDir, schema, cmdFile, cmd, logFile)
                //somehow liquibase doesn't commit.
                writeLogLine("commit;", logFile)
        }
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
            lines.stream().filter{ l -> !((String)l).startsWith("[INFO]")}.each { erLine -> println("--[ERROR]"+erLine)}
            System.exit(1)
        }
    }

    def handleOutput(sout, schemaDir, schema, cmdFile, cmd, logFile){
        Collection<String> lines = sout.toString().split("\\n")
        lines.stream().each{it ->
            if(it.toString().startsWith("LoggingExecutor")
            || it.toString().startsWith("Operation is successfully completed.")
            || it.toString().startsWith("Using temp directory:")) {
                writeLogLine("-- " + it, logFile)
            }else if(it.toString().startsWith("Processing has failed")){
                writeLogLine("--[ERROR] " + it , logFile)
                /// System.exit(1)
            }else if(it.toString().contains("_bad_") && it.toString().trim().endsWith(".xml")){
                // bad objects are materialized views or other views,
                // that are not generated appropiately and need to be used with genobject
                handleBadObjects(it.toString(), schemaDir, schema, cmdFile, cmd, logFile)
            }else{
                writeLogLine(it, logFile)
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
        writeLogLine("<include file=\"${xmlFileName}\"/><!-- [ERROR_NEEDS_SORTING][$schema][$name][Build is now deferred was $buildType]" + xmlFileName  + " add this to $schemaDir.absolutePath/controller.xml -->", logFile)
    }

    def writeLogLine(content, logFile){
        logFile << """$content\n"""
        println(content)
    }
}