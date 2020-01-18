import com.sunopsis.dwg.DwgObject
import oracle.odi.core.OdiInstance
import oracle.odi.core.service.vcs.generic.OdiObjectInfo
import oracle.odi.core.service.vcs.generic.adapter.OdiVcsAdapterType
import oracle.odi.core.service.vcs.generic.adapter.impl.OdiVcsAdapterFactory
import oracle.odi.core.service.vcs.generic.adapter.impl.OdiVcsRepositoryInfo
import oracle.odi.core.service.vcs.generic.delegate.OdiVcsDelegate
import oracle.odi.core.service.vcs.generic.exceptions.OdiVcsException
import oracle.odi.core.service.vcs.generic.utils.OdiVcsUtils
import oracle.odi.core.service.vcs.git.adapter.impl.OdiGitRepositoryInfo
import oracle.odi.core.service.vcs.git.adapter.impl.OdiJGitAdapter

/**
 * @since 8/22/19 \n
 */

class CommitToGit {

    public commit() {

        def repositoryUrl = 'ssh://git@mgit001:7999/dwh/odi.git'
        def privateKeyFile = '/home/odi/.ssh/id_rsa'
        def gitDirectory = '/home/odi/git/dwh/odi'
        def includeDependencies = false
        def regenerateScen = false
        def odiInstance = OdiConfig.instance.getOdiInstance()

        def wrl = '/u01/app/odi' //?
        def walletPwd = 'xxxxx!'.toCharArray()

        OdiGitRepositoryInfo repoInfo = new OdiGitRepositoryInfo()
        repoInfo.setAuthenticationType(OdiGitRepositoryInfo.GIT_AUTH_TYPE.GIT_SSH)
/*
        HTTP_BASIC("basic", "http://<host>[:<port>]/<git-repository>", "Http Basic"),
        HTTPS_SSL("ssl", "https://<host>[:<port>]/<git-repository>", "SSL"),
        GIT_BASIC("git", "git://<host>[:<port>]/<git-repository>", "Git Basic"),
        GIT_SSH("ssh", "ssh://<username>@<host>[:<port>]/<disk-path-of-git-repo>", "SSH"),
        FILE("file", "file://<disk-path-of-git-repo>", "File based"),
 */
        repoInfo.setRepositoryUrl(repositoryUrl)
        //repoInfo.setUserName(this.getUserName());
        //repoInfo.setUserPassword(this.getUserPassword());
        repoInfo.setPrivateKeyFile(new File(privateKeyFile))
        //repoInfo.setPassPhrase(this.getPassPhrase());
        repoInfo.gitDirectory = new File(gitDirectory)
        //repoInfo.setConnectionName(this.getConnectionName());
        //repoInfo.setCertificatePath(this.getCertificatePath());
        //repoInfo.setCertPassPhrase(this.getCertPassPhrase());
        //repoInfo.setProxyUrl(this.getProxyUrl());
        //repoInfo.setProxyPort(this.getProxyPort());
        //repoInfo.setProxyUserName(this.getProxyUserName());
        //repoInfo.setProxyUserPassword(this.getProxyUserPassword());
        boolean isSaveLoginCredPwd = true

        OdiVcsAdapterFactory odiVcsAdapterFactory = OdiVcsAdapterFactory.getInstance()
        OdiJGitAdapter odiVcsAdapter = odiVcsAdapterFactory.createVcsAdapter(OdiVcsAdapterType.Git_Jgit)
        odiVcsAdapter.init()
        odiVcsAdapter.pullFromRemoteRepository(repoInfo)
        assert !odiInstance.closed
        OdiVcsDelegate odiVcsDelegate = new OdiVcsDelegate(odiInstance)
        odiVcsDelegate.createConnection(wrl, walletPwd, repoInfo, isSaveLoginCredPwd)
        List<OdiObjectInfo> modified = odiVcsDelegate.getAllPendingChangesInRepository(repoInfo)
        odiVcsDelegate.createVersion( repoInfo, modified,  "modified",  includeDependencies,  regenerateScen)
        odiVcsAdapter.pushToRemoteRepository(repoInfo, "master")

        assert !odiInstance.closed
        Collection<DwgObject> nonVersioned = OdiVcsUtils.getNonVersionControlledObjects(odiInstance)
        odiVcsDelegate.addToVcs(repoInfo, nonVersioned, "created", includeDependencies, regenerateScen) // this matches
        odiVcsAdapter.pushToRemoteRepository(repoInfo, "master")
    }
}