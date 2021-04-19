# jodi.one - Java Oracle Data Integration #1

- jodi.one is a privately owned Dutch Based Software Company and not affiliated with Oracle Corporation

## jodi.one/jodi

* Provides ETL automation in Oracle Data Integrator
* Extensive API to create / generate ODI Objects (Variables, Constraints, Mappings, Packages, Scenarios, Loadplans,
  Deployment Archives)
* Provides Pattern based ETL automation

### CommandLineArguments;

*

see <a href="https://github.com/jodi-one/jodi/blob/master/jodi_base/src/main/java/one/jodi/base/bootstrap/BaseCmdlineArgumentProcessor.java">
one.jodi.base.bootstrap.BaseCmdlineArgumentProcessor<a/>

* below is an incomplete list, for the complete list see link above.

<table><tr><td>Name</td><td>Description</td><td>Action for JodiController</td></tr>
<tr><td>OdiController</td><td>General command line wrapper around the jodi controller (see list of
                              command above). The scripts below execute specific actions (e.g. -a action)
                              of this script.</td><td></td></tr>
<tr><td>OdiAlterTable</td><td>Executes alter tables in ODI, a step in the ETLS process. Alters keys if not
                              defined in DB, sets OLAP type of table, alters ROWID columns.
                              </td><td>-a atb -c odi.properties</td></tr>
<tr><td>OdiCheckTable</td><td>Executes check tables, a step in the ETLS process. Check alternate key
                              (column of name W_TABLENAME_D_U1) is declared in target data store.
                              </td><td>-a cktb -c odi.properties</td></tr>
<tr><td>OdiCreateEtls</td><td>Generate complete ETLs. This includes the full functionality that includes
                              deletion and creation (or update) of mappings, package and scenario deletion
                              and recreation</td><td>-a etls -c odi.properties -m directory -p prefix -includeConstraints true/false</td></tr>
<tr><td>OdiCreateInterface</td><td>Generate mappings in ODI as defined in XML specifications; this is a step in
                                   the complete ETLS process.
                              </td><td>-a ct -c odi.properties -m directory</td></tr>
<tr><td>OdiCreatePackage</td><td>Generate packages in ODI defined in XML specification; this is a step in the
                                 ETLS process. The assumption is that all mappings include accurate
                                 sequence numbers in a flexfield "Package Sequence".
                              </td><td>-a cp -c odi.properties -m  directory</td></tr>
<tr><td>OdiSCD</td><td>Sets tables with EFFECTIVE_DATE to OLAP type SCD; this is a step in the
                       ETLS process.
                    </td><td>-a atbs -c odi.properties</td></tr>
<tr><td>OdiDeleteReferences</td><td>Deletes foreign key references from a model From command line</td><td>-a dr -c odi.properties -p "Inf " -m "xml" -srcmdl
                                                                                                                                              "JODI_DMT"</td></tr>
<tr><td>OdiExportLoadPlan</td><td>Export ODI load plan to an external XML-formatted file to the specified
                                  directory.
                                  The parameter defaultscenarionames determines the names of the
                                  scenarios exported. jodi 1.4 creates scenarios for mappings (not generated
                                  in ODI 11), packages, procedures and variables with their default names. To
                                  reference scenarios from mappings, packages, procedures and variables, the
                                  default name for the scenarios must be specified. The loadplan export service
                                  can optionally translate scenarios named with non-default names and allow
                                  re-creation.</td><td>-a lpi -m directory -defaultscenarionames
                                                                                         true/false -c properties</td></tr>
<tr><td>OdiCreateLoadPlan</td><td>Create (import) ODI Load Plan from jodi's Load Plan specification.
                                  </td><td>-a lp -m directory -c properties</td></tr>
<tr><td>OdiPrintLoadPlan</td><td>Print out ODI Load Plan.
                                 Output is written to the Java logger. In order to view the output the
                                 conf/log4j.properties file in the jodi distribution will need to be configured from
                                 default as follows
                                 log4j.rootLogger= INFO, jodi, warnfile</td><td>-a lpp -c propertie</td></tr>
<tr><td>OdiExportConstraints</td><td>Export all constraints found in ODI repository. Constraints will be externalized
                                     to the Constraints.xml file found at the root of metadata directory. If the file is
                                     present it will be overwritten. The export DBConstraints option is used to
                                     prevent externalization of constraints that have the "In DB" option set; by
                                     default it is set to false.
                                     </td><td>-a expcon -m directory -exportDBConstraints
                                                                                                       true/false<</td></tr>
<tr><td>OdiDeleteConstraints</td><td>Delete all constraints specified by one or more constraint files found in the
                                     metadata directory. Note that constraints not specified by the file will not be
                                     deleted.
                                    </td><td>-a delcon -m directory</td></tr>
<tr><td>OdiCreateContraints</td><td>Create (import) condition, key and reference constraints from one or more
                                    constraint files found in the metadata directory.
                                    </td><td>-a crtcon -m directory</td></tr>
<tr><td>OdiCreateSequences</td><td>Create (import) sequences from one or more constraint files found in metadata
                                   directory.
                                  </td><td>-a crtseq -c properties -p "Init " -m directory</td></tr>
<tr><td>OdiDeleteSequences</td><td>Delete sequences specified by one more sequences files found in metadata
                                   directory.
                                   </td><td>-a delseq -c properties
                                                                               -p "Init " -m directory</td></tr>
<tr><td>OdiExportSequences</td><td>Export all sequences found in ODI to XML
                                   </td><td>-a expseq -c properties
                                                                               -p "Init " -m directory</td></tr>
<tr><td>OdiCreateVariables</td><td>Create (import) variables from one or more variable files found in metadata
                                   directory
                                  </td><td>-a crtvar</td></tr>
<tr><td>OdiDeleteVariables</td><td>Delete all variables described by one or more variables files found in metadata
                                   directory.
                                   </td><td>-a delvar -c properties
                                                                               -p "Init " -m directory</td></tr>
<tr><td>OdiExportVariables</td><td>Export all variables in ODI to XML
                                   </td><td>-a expvar -c properties
                                                                               -p "Init " -m directory</td></tr>
<tr><td>OdiCreateProcedures</td><td>Create procedures from one or more procedure files found in metadata
                                    directory
                                    </td><td>-a crtproc -c properties
                                                                                 -p "Init " -m directory</td></tr>
<tr><td>OdiDeleteProcedures</td><td>Delete all procedures described by one or more procedure files found in
                                    metadata directory.
                                   </td><td>-a delproc -c properties
                                                                                -p "Init " -m directory</td></tr>
<tr><td>OdiValidate</td><td>Validates the transformation specifications without creating or deleting
                            Mappings in ODI 12c. Warnings and error messages are printed to log4j logs.</td><td>-a vldt</td></tr>
</table>

### Actions

*

See <a href="https://github.com/jodi-one/jodi/blob/master/jodi_core/src/main/java/one/jodi/bootstrap/EtlRunConfig.java">
one.jodi.bootstrap.ActionType</a>

<h2>Building a binaries</h2>

* use this link to install ojdbc.jar into
  maven: https://mkyong.com/maven/how-to-add-oracle-jdbc-driver-in-your-maven-local-repository/
* See commands in createDist.bat
* You'll need jodi_core\build\distributions\jodi_core-x.x.x-x-x-x-SNAPSHOT.zip
* and plugins you'll need for providing custom plugins
  jodi_plugins\build\distributions\jodi_plugins-x.x.x-x-x-x-SNAPSHOT.zip
* When using it in an idea; copy all jars of jodi_core and add the one jar from jodi_plugins to the class path
* To use as binaries unzip jodi_core and add the jar from jodi_plugins to the lib folder
* create a configuration file e.g: jodi_qa/src/test/resources/SampleC/conf/SampleC.properties
* create a metadata folder e.g: jodi_qa/src/test/resources/SampleC/xml
* When using in IDEA call JodiController
* When using as binary call bin\jodi_core[.bat]

<h2>Examples</h2>

There are extensive examples in <a href="https://github.com/jodi-one/jodi/tree/master/jodi_qa/src/test/resources">
jodi_qa</a>.

<h3>In Groovy</h3>

<pre><code>
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
</code></pre>

### Development

There is a `.editorconfig` file available with settings for Code styling, most IDE's should pick it up automatically and
use the settings for the project it is in. Ensure your IDE has support enabled:

#### Intellij

- Go to _File_ -> _Settings_ (or use `ctrl-alt-S`) -> _Plugins_
- Look up plugin _Editorconfig_ (should be installed by default)
- Ensure it is installed and enabled
