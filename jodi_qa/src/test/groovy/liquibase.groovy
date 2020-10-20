

File resource = new File("/opt/git/jodi/jodi_qa/src/test/groovy")

def schemas = ["CHINOOK","DWH_DMT","DWH_STO","DWH_STI","DWH_CON_CHINOOK","DWH_CON","DWH_SRC","DWH_STG"]
def urlDev = 'DB202007280549_high?TNS_ADMIN=/opt/git/opc/src/main/resources/wallet/'

schemas.each{
    schema ->
        File schemaDir = new File(resource, schema)
        schemaDir.mkdirs()
       // """cd $schema""".execute()
        File cmdFile= new File(resource, "cmd.sql")
        cmdFile.write("""!cd $schema""".toString())
        cmdFile << 'lb genSchema -context legacy'
        def OCI_JODI_PWD = System.getProperty("OCI_JODI_PWD")
        def cmd = """sql $schema/'$OCI_JODI_PWD'@$urlDev @cmd.sql"""
        println(cmd)
        cmd.execute()
        //"""cd ..""".execute()
}