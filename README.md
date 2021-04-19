# jodi.one - Java Oracle Data Integration #1

- jodi.one is a privately owned Dutch Based Software Company and not affiliated with Oracle Corporation

## jodi.one/jodi

* Provides ETL automation in Oracle Data Integrator
* Extensive API to create / generate ODI Objects (Variables, Constraints, Mappings, Packages, Scenarios, Loadplans,
  Deployment Archives)
* Provides Pattern based ETL automation

### CommandLineArguments;

See [one.jodi.base.bootstrap.BaseCmdlineArgumentProcessor](jodi_base/src/main/java/one/jodi/base/bootstrap/BaseCmdlineArgumentProcessor.java)

Below is an incomplete list, for the complete list see link above.

| Name | Description | Action for JodiController | 
                | --- | --- | --- |
| OdiController | General command line wrapper around the jodi controller (see list of command above). The scripts below execute specific actions (e.g. -a action) of this script. | | 
| OdiAlterTable | Executes alter tables in ODI, a step in the ETLS process. Alters keys if not defined in DB, sets OLAP type of table, alters ROWID columns. | -a atb -c odi.properties |
| OdiCheckTable | Executes check tables, a step in the ETLS process. Check alternate key (column of name W_TABLENAME_D_U1) is declared in target data store. | > -a cktb -c odi.properties |
| OdiCreateEtls | Generate complete ETLs. This includes the full functionality that includes deletion and creation (or update) of mappings, package and scenario deletion and recreation | -a etls -c odi.properties -m directory -p prefix -includeConstraints true/false |
| OdiCreateInterface | Generate mappings in ODI as defined in XML specifications; this is a step in the complete ETLS process. | -a ct -c odi.properties -m directory |
| OdiCreatePackage | Generate packages in ODI defined in XML specification; this is a step in the ETLS process. The assumption is that all mappings include accurate sequence numbers in a flexfield "Package Sequence". | -a cp -c odi.properties -m  directory
| OdiSCD | Sets tables with EFFECTIVE_DATE to OLAP type SCD; this is a step in the ETLS process. | -a atbs -c odi.properties
| OdiDeleteReferences | Deletes foreign key references from a model From command line | -a dr -c odi.properties -p "Inf " -m "xml" -srcmdl "JODI_DMT" |
| OdiExportLoadPlan | Export ODI load plan to an external XML-formatted file to the specified directory. <br> The parameter defaultscenarionames determines the names of the scenarios exported. jodi 1.4 creates scenarios for mappings (not generated in ODI 11), packages, procedures and variables with their default names. <br> To reference scenarios from mappings, packages, procedures and variables, the default name for the scenarios must be specified. The loadplan export service can optionally translate scenarios named with non-default names and allow re-creation. | -a lpi -m directory -defaultscenarionames true/false -c properties |
| OdiCreateLoadPlan | Create (import) ODI Load Plan from jodi's Load Plan specification. | -a lp -m directory -c properties |
| OdiPrintLoadPlan | Print out ODI Load Plan. <br> Output is written to the Java logger. In order to view the output the conf/log4j.properties file in the jodi distribution will need to be configured from default as follows: <br> `log4j.rootLogger= INFO, jodi, warnfile` | -a lpp -c propertie |
| OdiExportConstraints | Export all constraints found in ODI repository. Constraints will be externalized to the Constraints.xml file found at the root of metadata directory. If the file is present it will be overwritten. The export DBConstraints option is used to prevent externalization of constraints that have the "In DB" option set; by default it is set to false. | -a expcon -m directory -exportDBConstraints true/false< | 
| OdiDeleteConstraints | Delete all constraints specified by one or more constraint files found in the metadata directory. Note that constraints not specified by the file will not be deleted. | -a delcon -m directory | 
| OdiCreateContraints | Create (import) condition, key and reference constraints from one or more constraint files found in the metadata directory. | -a crtcon -m directory | 
| OdiCreateSequences | Create (import) sequences from one or more constraint files found in metadata directory. | -a crtseq -c properties -p "Init " -m directory |
| OdiDeleteSequences | Delete sequences specified by one more sequences files found in metadata directory. | -a delseq -c properties -p "Init " -m directory |
| OdiExportSequences | Export all sequences found in ODI to XML | -a expseq -c properties -p "Init " -m directory | 
| OdiCreateVariables | Create (import) variables from one or more variable files found in metadata directory | -a crtvar | 
| OdiDeleteVariables | Delete all variables described by one or more variables files found in metadata directory. | -a delvar -c properties -p "Init " -m directory | 
| OdiExportVariables | Export all variables in ODI to XML | -a expvar -c properties -p "Init " -m directory | 
| OdiCreateProcedures | Create procedures from one or more procedure files found in metadata directory | -a crtproc -c properties -p "Init " -m directory | 
| OdiDeleteProcedures | Delete all procedures described by one or more procedure files found in metadata directory. | -a delproc -c properties -p " Init " -m directory | 
| OdiValidate | Validates the transformation specifications without creating or deleting Mappings in ODI 12c. Warnings and error messages are printed to log4j logs. | -a vldt |

### Actions

See [one.jodi.bootstrap.ActionType](jodi_core/src/main/java/one/jodi/bootstrap/EtlRunConfig.java)

### Building a binaries

* Use this link to install ojdbc.jar into
  maven: https://mkyong.com/maven/how-to-add-oracle-jdbc-driver-in-your-maven-local-repository
* See commands in `createDist.bat`
* You'll need `jodi_core\build\distributions\jodi_core-x.x.x-x-x-x-SNAPSHOT.zip`
* and plugins you'll need for providing custom plugins
  `jodi_plugins\build\distributions\jodi_plugins-x.x.x-x-x-x-SNAPSHOT.zip`
* When using it in an IDE, copy all jars of _jodi_core_ and add the one jar from _jodi_plugins_ to the class path
* To use as binaries unzip _jodi_core_ and add the jar from _jodi_plugins_ to the lib folder
* Create a configuration file e.g: `jodi_qa/src/test/resources/SampleC/conf/SampleC.properties`
* Create a metadata folder e.g: `jodi_qa/src/test/resources/SampleC/xml`
* When using in IDE call _JodiController_
* When using as binary call _bin\jodi_core[.bat]_

### Examples

There are extensive examples in [jodi_qa](jodi_qa/src/test/resources).

### In Groovy

      import one.jodi.base.config.PasswordConfigImpl
      import one.jodi.bootstrap.JodiController
      
      JodiController controller = new JodiController(true)
      List<String> argList = new ArrayList<String>()
      argList.add("-a")
      argList.add("etls")
      argList.add("-c")
      argList.add("jodi_qa/src/test/resources/SampleC/conf/SampleC.properties")
      argList.add("-pw")
      argList.add(new PasswordConfigImpl().getOdiUserPassword())
      argList.add("-mpw")
      argList.add(new PasswordConfigImpl().getOdiMasterRepoPassword())
      argList.add("-devmode")
      argList.add("-p")
      argList.add("Real Time ")
      argList.add("-m")
      argList.add("jodi_qa/src/test/resources/SampleC/xml")
      controller.run(argList.toArray(new String[0]))
      print controller.getErrorReport()

### Development

#### Line endings

There is a difference in handling of line endings between Windows, Linux and Mac OS. To circumvent any mismatches, it is
best to configure Git to always transform line endings to a single linefeed (Linux-style) when committing, so that the
files in the repository stay compatible. It is best to make it a global setting on your machine so it applies to all
repositories:

On windows:

`git config --global core.autocrlf true`

On Linux and OS X:

`git config --global core.autocrlf input`

Src: https://docs.github.com/en/github/getting-started-with-github/configuring-git-to-handle-line-endings

#### Code style

There is an `.editorconfig` file available with settings for code styling, most IDE's should pick it up automatically
and use the settings for the project it is in. Ensure your IDE has support enabled:

###### Intellij

- Go to _File_ -> _Settings_ (or use `ctrl-alt-S`) -> _Plugins_
- Look up plugin _EditorConfig_ (should be installed by default)
- Ensure it is installed and enabled
