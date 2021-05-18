package one.jodi.base.config;

public class PasswordConfigImpl implements PasswordConfig {

   @Override
   public String getOdiMasterRepoPassword() {
      return System.getProperty("ODI_REPO_PWD");
   }

   @Override
   public String getOdiUserPassword() {
      return System.getProperty("ODI_USER_PWD");
   }

   @Override
   public String getOracleTestDBPassword() {
      return System.getProperty("OCI_PWD");
   }

   @Override
   public String getDeploymentArchivePassword() {
      return System.getProperty("ODI_DAPWD");
   }

   @Override
   public String getAdminDBPassword() {
      return System.getProperty("OCI_ADMIN_PWD");
   }
}