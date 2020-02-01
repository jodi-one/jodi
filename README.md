# jodi.one - Java Oracle Data Integration #1 
- jodi.one is a privately owned Dutch Based Software Company and not affiliated with Oracle Corporation

## jodi.one/jodi
* Provides ETL automation in Oracle Data Integrator  
* Extensive API to create / generate ODI Objects (Variables, Constraints, Mappings, Packages, Scenarios, Loadplans, Deployment Archives)
* Provides Pattern based ETL automation  

### CommandLineArguments;
* see <a href="https://github.com/jodi-one/jodi/blob/master/jodi_base/src/main/java/one/jodi/base/bootstrap/BaseCmdlineArgumentProcessor.java">one.jodi.base.bootstrap.BaseCmdlineArgumentProcessor<a/>

### Actions
* See <a href="https://github.com/jodi-one/jodi/blob/master/jodi_core/src/main/java/one/jodi/bootstrap/EtlRunConfig.java">one.jodi.bootstrap.ActionType</a>

<h2>Building a binaries</h2>

* See commands in createDist.bat
* You'll need  jodi_core\build\distributions\jodi_core-x.x.x-x-x-x-SNAPSHOT.zip
* and plugins you'll need for providing custom plugins jodi_plugins\build\distributions\jodi_plugins-x.x.x-x-x-x-SNAPSHOT.zip
* When using it in an idea; copy all jars of jodi_core and add the one jar from jodi_plugins to the class path
* To use as binaries unzip jodi_core and add the jar from jodi_plugins to the lib folder
* create a configuration file e.g: jodi_qa/src/test/resources/SampleC/conf/SampleC.properties
* create a metadata folder e.g: jodi_qa/src/test/resources/SampleC/xml
* When using in IDEA call JodiController
* When using as binary call bin\jodi_core[.bat]

<h2>Examples</h2>

<h3>Action -etls</h3>

<h4>In Groovy</h4>
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