.\gradlew.bat clean increaseBuildNumber
# build must be on a separate line to pickup new build number
# build all code related to jodi_core but test all related project separately
.\gradlew.bat -P distribution=true jodi_core:build -x test
.\gradlew.bat -P distribution=true jodi_base:findbugsMain jodi_base:findbugsTest jodi_base:test jodi_odi12:findbugsMain jodi_odi12:findbugsTest jodi_odi12:test -x test