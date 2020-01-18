package one.jodi.odi12.folder;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.etl.service.ResourceNotFoundException;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.finder.IOdiFolderFinder;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Odi12FolderServiceProvider {

    final static Logger logger =
            LogManager.getLogger(Odi12FolderServiceProvider.class);

    private final String ERROR_MESSAGE_85000 =
            "Unable to find ODI project %1$s.";
    private final String ERROR_MESSAGE_85010 =
            "Unable to find folders in ODI project with code %1$s.";

    private final OdiInstance odiInstance;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public Odi12FolderServiceProvider(final OdiInstance odiInstance,
                                      final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.odiInstance = odiInstance;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Cached
    protected OdiProject findProject(final String projectCode) {
        final IOdiProjectFinder finder =
                ((IOdiProjectFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiProject.class));
        final OdiProject project = finder.findByCode(projectCode);
        return project;
    }

    private Collection<OdiFolder> findAllFoldersInProject(final String projectCode)
            throws ResourceNotFoundException {
        assert (projectCode != null && !projectCode.isEmpty());
        final IOdiFolderFinder finder =
                ((IOdiFolderFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiFolder.class));
        final Collection<OdiFolder> folders = finder.findByProject(projectCode);
        if (folders == null) {
            String msg = this.errorWarningMessages
                    .formatMessage(85010, ERROR_MESSAGE_85010, this.getClass(),
                            projectCode);
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
            throw new ResourceNotFoundException(msg);
        }
        return folders;
    }

    private Map<String, OdiFolder> findFoldersInProject(final String projectCode)
            throws ResourceNotFoundException {
        return findAllFoldersInProject(projectCode).stream()
                .collect(Collectors.toMap(f -> Odi12FolderHelper.getFolderPath(f),
                        f -> f));
    }

    private OdiFolder addFolder(final String folderName, final String projectCode)
            throws ResourceNotFoundException {
        assert (odiInstance.getTransactionalEntityManager().isOpen());
        OdiProject project = findProject(projectCode);
        if (project == null) {
            String msg = this.errorWarningMessages
                    .formatMessage(85000, ERROR_MESSAGE_85000, this.getClass(),
                            projectCode);
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
            throw new ResourceNotFoundException(msg);
        }
        OdiFolder folder = new OdiFolder(project, folderName);
        odiInstance.getTransactionalEntityManager().persist(folder);
        logger.info(String.format("Created folder '%s'.", folderName));
        return folder;
    }

    private OdiFolder addSubFolder(final OdiFolder parent, final String folderName) {
        assert (odiInstance.getTransactionalEntityManager().isOpen());
        assert (parent != null);

        OdiFolder folder = new OdiFolder(parent, folderName);
        odiInstance.getTransactionalEntityManager().persist(folder);
        logger.info(String.format("Created subfolder '%1$s' in folder '%2$s'.",
                folderName, parent.getName()));
        return folder;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected Map<String, OdiFolder> addSubFolders(final OdiFolder parentFolder,
                                                   final String folderPathToParent,
                                                   final List<String> missingSubFolderPath,
                                                   final String projectCode)
            throws ResourceNotFoundException {
        assert (missingSubFolderPath != null);
        Map<String, OdiFolder> newFolderMap = new HashMap<>();

        List<String> remainingPath = missingSubFolderPath;
        OdiFolder parent = parentFolder;
        String initialPath = folderPathToParent;
        if (parentFolder == null) {
            String parentFolderName = missingSubFolderPath.get(0);
            assert (parentFolderName != null && !parentFolderName.isEmpty());
            parent = addFolder(parentFolderName, projectCode);
            remainingPath = missingSubFolderPath.subList(1, missingSubFolderPath.size());
            newFolderMap.put(parentFolderName, parent);
            initialPath = parentFolderName;
        }

        StringBuilder path = new StringBuilder(initialPath);
        for (String subfolderName : remainingPath) {
            assert (parent != null);
            assert (subfolderName != null && !subfolderName.isEmpty());
            path.append("/")
                    .append(subfolderName);
            parent = addSubFolder(parent, subfolderName);
            newFolderMap.put(path.toString(), parent);
        }
        return newFolderMap;
    }

    private int findLastExistingFolder(final List<String> folderNames,
                                       final Map<String, OdiFolder> folders) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        int lastIncluded = -1;
        for (int i = 0; i < folderNames.size(); i++) {
            sb.append(sep).append(folderNames.get(i));
            if (folders.get(sb.toString()) == null) {
                break;
            }
            sep = "/";
            lastIncluded = i;
        }
        return lastIncluded;
    }

    private List<String> getFolderPathList(final String folderPath) {
        assert (folderPath != null && !folderPath.isEmpty() && !folderPath.contains("//"));
        return Arrays.asList(folderPath.split("/"));
    }

    public Map<String, OdiFolder> findOrCreateFolders(final List<String> folderPaths,
                                                      final String projectCode) {
        Map<String, OdiFolder> folders;
        try {
            folders = findFoldersInProject(projectCode);
        } catch (ResourceNotFoundException re) {
            throw new UnRecoverableException("Unable to create procedures.", re);
        }

        Map<String, List<String>> folderPathMap =
                folderPaths.stream()
                        .filter(p -> p != null && !p.isEmpty())
                        .collect(Collectors.toMap(p -> p,
                                p -> getFolderPathList(p)));

        for (Entry<String, List<String>> entry : folderPathMap.entrySet()) {
            List<String> path = entry.getValue();
            int last = findLastExistingFolder(path, folders);
            String pathToExisting = String.join("/", path.subList(0, last + 1));
            OdiFolder parent = folders.get(pathToExisting); // may be null if last is -1
            try {
                Map<String, OdiFolder> newFolders =
                        addSubFolders(parent, pathToExisting,
                                path.subList(last + 1, path.size()), projectCode);
                folders.putAll(newFolders);
            } catch (ResourceNotFoundException re) {
                throw new UnRecoverableException("Error Accessing resources.", re);
            }
        }

        // collect paths defined by the folderPath parameter
        return folderPaths.stream()
                .filter(n -> folders.get(n) != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(n -> n,
                                n -> folders.get(n)),
                        Collections::unmodifiableMap));
    }

    // find folders by name and afterwards test if path is correct
    private Optional<OdiFolder> getFolderByName(final List<String> folderNames,
                                                final String projectCode) {
        assert (projectCode != null && !projectCode.isEmpty());
        assert (folderNames.size() > 0);

        final String folderPath = String.join("/", folderNames);
        String folderName = folderNames.get(folderNames.size() - 1);
        final IOdiFolderFinder finder =
                ((IOdiFolderFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiFolder.class));
        final Collection<OdiFolder> folders = finder.findByName(folderName, projectCode);

        Optional<OdiFolder> found = Optional.empty();
        if (!folders.isEmpty()) {
            found = folders.stream()
                    .filter(f -> Odi12FolderHelper.getFolderPath(f).equals(folderPath))
                    .findFirst();
        }
        return found;
    }

    public Optional<OdiFolder> findFolderByName(final String folderPath,
                                                final String projectCode) {
        assert (projectCode != null && !projectCode.isEmpty());
        assert (folderPath != null && !folderPath.isEmpty() && !folderPath.contains("//"));
        return getFolderByName(Arrays.asList(folderPath.split("/")), projectCode);
    }

    public List<OdiFolder> findFolderAndSubfolders(final String folderPath,
                                                   final String projectCode) {
        try {
            return findFoldersInProject(projectCode)
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().equals(folderPath) ||
                            e.getKey().startsWith(folderPath + "/"))
                    .map(e -> e.getValue())
                    .collect(Collectors.toList());
        } catch (ResourceNotFoundException e) {
            throw new UnRecoverableException("Error Accessing resources.", e);
        }
    }

}