/*
 * Copyright (c) 2016 Boyter Online Services
 *
 * Use of this software is governed by the Fair Source License included
 * in the LICENSE.TXT file
 */


package com.searchcode.app.jobs;

// Useful for the future
// http://stackoverflow.com/questions/1685228/how-to-cat-a-file-in-jgit

import com.searchcode.app.config.Values;
import com.searchcode.app.dto.CodeIndexDocument;
import com.searchcode.app.dto.CodeOwner;
import com.searchcode.app.dto.RepositoryChanged;
import com.searchcode.app.model.RepoResult;
import com.searchcode.app.service.CodeIndexer;
import com.searchcode.app.service.CodeSearcher;
import com.searchcode.app.service.Singleton;
import com.searchcode.app.util.*;
import com.searchcode.app.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.quartz.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This job is responsible for pulling and indexing git repositories
 *
 * TODO add more tests as they are lacking
 * TODO use inheritance/template methods to combine the common stuff between this and SVN job then subclass
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class IndexGitRepoJob implements Job {

    private int SLEEPTIME = 5000;
    private boolean LOWMEMORY;
    private String GITBINARYPATH;
    private boolean USESYSTEMGIT;
    public int MAXFILELINEDEPTH = Helpers.tryParseInt(Properties.getProperties().getProperty(Values.MAXFILELINEDEPTH, Values.DEFAULTMAXFILELINEDEPTH), Values.DEFAULTMAXFILELINEDEPTH);

    public IndexGitRepoJob() {
        this.LOWMEMORY = true;
        this.GITBINARYPATH = Properties.getProperties().getProperty(Values.GITBINARYPATH, Values.DEFAULTGITBINARYPATH);
        this.USESYSTEMGIT = Boolean.parseBoolean(Properties.getProperties().getProperty(Values.USESYSTEMGIT, Values.DEFAULTUSESYSTEMGIT));

        File f = new File(this.GITBINARYPATH);
        if (USESYSTEMGIT && !f.exists()) {
            Singleton.getLogger().warning("\n///////////////////////////////////////////////////////////////////////////\n// Property git_binary_path in properties file appears to be incorrect.  //\n// Please check the path. Falling back to internal git implementation.   //\n///////////////////////////////////////////////////////////////////////////");

            this.USESYSTEMGIT = false;
        }
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        while(CodeIndexer.shouldPauseAdding()) {
            Singleton.getLogger().info("Pausing parser.");
            return;
        }

        // Pull the next repo to index from the queue
        UniqueRepoQueue repoQueue = Singleton.getUniqueGitRepoQueue();

        RepoResult repoResult = repoQueue.poll();
        AbstractMap<String, Integer> runningIndexGitRepoJobs = Singleton.getRunningIndexRepoJobs();

        if (repoResult != null && !runningIndexGitRepoJobs.containsKey(repoResult.getName())) {
            Singleton.getLogger().info("Indexing " + repoResult.getName());
            try {
                runningIndexGitRepoJobs.put(repoResult.getName(), (int) (System.currentTimeMillis() / 1000));

                JobDataMap data = context.getJobDetail().getJobDataMap();

                String repoName = repoResult.getName();
                String repoRemoteLocation = repoResult.getUrl();
                String repoUserName = repoResult.getUsername();
                String repoPassword = repoResult.getPassword();
                String repoBranch = repoResult.getBranch();

                String repoLocations = data.get("REPOLOCATIONS").toString();
                this.LOWMEMORY = Boolean.parseBoolean(data.get("LOWMEMORY").toString());

                // Check if sucessfully cloned, and if not delete and restart
                boolean cloneSucess = checkCloneUpdateSucess(repoLocations + repoName);
                if (cloneSucess == false) {
                    // Delete the folder and delete from the index
                    try {
                        FileUtils.deleteDirectory(new File(repoLocations + "/" + repoName + "/"));
                        CodeIndexer.deleteByReponame(repoName);
                    } catch (IOException ex) {
                        Singleton.getLogger().warning("ERROR - caught a " + ex.getClass() + " in " + this.getClass() + "\n with message: " + ex.getMessage());
                    }
                }
                deleteCloneUpdateSuccess(repoLocations + "/" + repoName);

                String repoGitLocation = repoLocations + "/" + repoName + "/.git/";

                File f = new File(repoGitLocation);
                boolean existingRepo = f.exists();
                boolean useCredentials = repoUserName != null && !repoUserName.isEmpty();
                RepositoryChanged repositoryChanged = null;

                if (existingRepo) {
                    repositoryChanged = this.updateGitRepository(repoName, repoRemoteLocation, repoUserName, repoPassword, repoLocations, repoBranch, useCredentials);
                } else {
                    repositoryChanged = this.cloneGitRepository(repoName, repoRemoteLocation, repoUserName, repoPassword, repoLocations, repoBranch, useCredentials);
                }

                // Write file indicating we have sucessfully cloned
                createCloneUpdateSuccess(repoLocations + "/" + repoName);
                // If the last index was not sucessful, then trigger full index
                boolean indexsuccess = checkIndexSucess(repoGitLocation);

                if (repositoryChanged.isChanged() || indexsuccess == false) {
                    Singleton.getLogger().info("Update found indexing " + repoRemoteLocation);
                    this.updateIndex(repoName, repoLocations, repoRemoteLocation, existingRepo, repositoryChanged);
                }
            }
            finally {
                // Clean up the job
                runningIndexGitRepoJobs.remove(repoResult.getName());
            }
        }
    }

    private void updateIndex(String repoName, String repoLocations, String repoRemoteLocation, boolean existingRepo, RepositoryChanged repositoryChanged) {
        String repoGitLocation = repoLocations + "/" + repoName;
        Path docDir = Paths.get(repoGitLocation);

        // Was the previous index sucessful? if not then index by path
        boolean indexsucess = checkIndexSucess(repoGitLocation);
        deleteIndexSuccess(repoGitLocation);

        if (!repositoryChanged.isClone() && indexsucess == false) {
            Singleton.getLogger().info("Failed to index " + repoName + " fully, performing a full index.");
        }

        if (repositoryChanged.isClone() || indexsucess == false) {
            Singleton.getLogger().info("Doing full index of files for " + repoName);
            this.indexDocsByPath(docDir, repoName, repoLocations, repoRemoteLocation, existingRepo);
        }
        else {
            Singleton.getLogger().info("Doing delta index of files " + repoName);
            this.indexDocsByDelta(docDir, repoName, repoLocations, repoRemoteLocation, repositoryChanged);
        }

        // Write file indicating that the index was sucessful
        Singleton.getLogger().info("Sucessfully processed writing index success for " + repoName);
        createIndexSuccess(repoGitLocation);
    }


    /**
     * Indexes all the documents in the repository changed file effectively performing a delta update
     * Should only be called when there is a genuine update IE something was indexed previously and
     * has has a new commit.
     */
    public void indexDocsByDelta(Path path, String repoName, String repoLocations, String repoRemoteLocation, RepositoryChanged repositoryChanged) {
        SearchcodeLib scl = Singleton.getSearchCodeLib(); // Should have data object by this point
        Queue<CodeIndexDocument> codeIndexDocumentQueue = Singleton.getCodeIndexQueue();
        String fileRepoLocations = FilenameUtils.separatorsToUnix(repoLocations);

        for(String changedFile: repositoryChanged.getChangedFiles()) {

            while(CodeIndexer.shouldPauseAdding()) {
                Singleton.getLogger().info("Pausing parser.");
                try {
                    Thread.sleep(SLEEPTIME);
                } catch (InterruptedException ex) {}
            }

            String[] split = changedFile.split("/");
            String fileName = split[split.length - 1];
            changedFile = fileRepoLocations + "/" + repoName + "/" + changedFile;

            String md5Hash = Values.EMPTYSTRING;
            List<String> codeLines = null;

            try {
                codeLines = Helpers.readFileLines(changedFile, this.MAXFILELINEDEPTH);
            } catch (IOException ex) {
                Singleton.getLogger().warning("ERROR - caught a " + ex.getClass() + " in " + this.getClass() +  "\n with message: " + ex.getMessage());
                break;
            }

            try {
                FileInputStream fis = new FileInputStream(new File(changedFile));
                md5Hash = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
                fis.close();
            } catch (IOException ex) {
                Singleton.getLogger().warning("Unable to generate MD5 for " + changedFile);
            }

            if(scl.isMinified(codeLines)) {
                Singleton.getLogger().info("Appears to be minified will not index  " + changedFile);
                break;
            }

            String languageName = scl.languageGuesser(changedFile, codeLines);
            String fileLocation = changedFile.replace(fileRepoLocations, Values.EMPTYSTRING).replace(fileName, Values.EMPTYSTRING);
            String fileLocationFilename = changedFile.replace(fileRepoLocations, Values.EMPTYSTRING);
            String repoLocationRepoNameLocationFilename = changedFile;


            String newString = getBlameFilePath(fileLocationFilename);
            List<CodeOwner> owners;
            if (this.USESYSTEMGIT) {
                owners = getBlameInfoExternal(codeLines.size(), repoName, fileRepoLocations, newString);
            }
            else {
                owners = getBlameInfo(codeLines.size(), repoName, fileRepoLocations, newString);
            }
            String codeOwner = scl.codeOwner(owners);


            if (codeLines != null) {
                if (this.LOWMEMORY) {
                    try {
                        CodeIndexer.indexDocument(new CodeIndexDocument(repoLocationRepoNameLocationFilename, repoName, fileName, fileLocation, fileLocationFilename, md5Hash, languageName, codeLines.size(), StringUtils.join(codeLines, " "), repoRemoteLocation, codeOwner));
                    } catch (IOException ex) {
                        Singleton.getLogger().warning("ERROR - caught a " + ex.getClass() + " in " + this.getClass() +  "\n with message: " + ex.getMessage());
                    }
                } else {
                    Singleton.incrementCodeIndexLinesCount(codeLines.size());
                    codeIndexDocumentQueue.add(new CodeIndexDocument(repoLocationRepoNameLocationFilename, repoName, fileName, fileLocation, fileLocationFilename, md5Hash, languageName, codeLines.size(), StringUtils.join(codeLines, " "), repoRemoteLocation, codeOwner));
                }
            }
        }

        for(String deletedFile: repositoryChanged.getDeletedFiles()) {
            Singleton.getLogger().info("Missing from disk, removing from index " + deletedFile);
            try {
                CodeIndexer.deleteByFileLocationFilename(deletedFile);
            } catch (IOException ex) {
                Singleton.getLogger().warning("ERROR - caught a " + ex.getClass() + " in " + this.getClass() +  "\n with message: " + ex.getMessage());
            }
        }
    }

    /**
     * Indexes all the documents in the path provided. Will also remove anything from the index if not on disk
     * Generally this is a slow update used only for the inital clone of a repository
     * NB this can be used for updates but it will be much slower as it needs to to walk the contents of the disk
     */
    public void indexDocsByPath(Path path, String repoName, String repoLocations, String repoRemoteLocation, boolean existingRepo) {
        SearchcodeLib scl = Singleton.getSearchCodeLib(); // Should have data object by this point
        List<String> fileLocations = new ArrayList<>();
        Queue<CodeIndexDocument> codeIndexDocumentQueue = Singleton.getCodeIndexQueue();

        // Convert once outside the main loop
        String fileRepoLocations = FilenameUtils.separatorsToUnix(repoLocations);
        boolean lowMemory = this.LOWMEMORY;
        boolean useSystemGit = this.USESYSTEMGIT;

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    while(CodeIndexer.shouldPauseAdding()) {
                        Singleton.getLogger().info("Pausing parser.");
                        try {
                            Thread.sleep(SLEEPTIME);
                        } catch (InterruptedException ex) {}
                    }

                    // Convert Path file to unix style that way everything is easier to reason about
                    String fileParent = FilenameUtils.separatorsToUnix(file.getParent().toString());
                    String fileToString = FilenameUtils.separatorsToUnix(file.toString());
                    String fileName = file.getFileName().toString();
                    String md5Hash = Values.EMPTYSTRING;

                    if (fileParent.endsWith("/.git") || fileParent.contains("/.git/")) {
                        return FileVisitResult.CONTINUE;
                    }


                    List<String> codeLines;
                    try {
                        codeLines = Helpers.readFileLines(fileToString, MAXFILELINEDEPTH);
                    } catch (IOException ex) {
                        return FileVisitResult.CONTINUE;
                    }

                    try {
                        FileInputStream fis = new FileInputStream(new File(fileToString));
                        md5Hash = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
                        fis.close();
                    } catch (IOException ex) {
                        Singleton.getLogger().warning("Unable to generate MD5 for " + fileToString);
                    }

                    // is the file minified?
                    if(scl.isMinified(codeLines)) {
                        Singleton.getLogger().info("Appears to be minified will not index  " + fileToString);
                        return FileVisitResult.CONTINUE;
                    }

                    String languageName = scl.languageGuesser(fileName, codeLines);
                    String fileLocation = fileToString.replace(fileRepoLocations, Values.EMPTYSTRING).replace(fileName, Values.EMPTYSTRING);
                    String fileLocationFilename = fileToString.replace(fileRepoLocations, Values.EMPTYSTRING);
                    String repoLocationRepoNameLocationFilename = fileToString;


                    String newString = getBlameFilePath(fileLocationFilename);
                    List<CodeOwner> owners;
                    if (useSystemGit) {
                        owners = getBlameInfoExternal(codeLines.size(), repoName, fileRepoLocations, newString);
                    }
                    else {
                        owners = getBlameInfo(codeLines.size(), repoName, fileRepoLocations, newString);
                    }

                    String codeOwner = scl.codeOwner(owners);


                    // If low memory don't add to the queue, just index it directly
                    if (lowMemory) {
                        CodeIndexer.indexDocument(new CodeIndexDocument(repoLocationRepoNameLocationFilename, repoName, fileName, fileLocation, fileLocationFilename, md5Hash, languageName, codeLines.size(), StringUtils.join(codeLines, " "), repoRemoteLocation, codeOwner));
                    }
                    else {
                        Singleton.incrementCodeIndexLinesCount(codeLines.size());
                        codeIndexDocumentQueue.add(new CodeIndexDocument(repoLocationRepoNameLocationFilename, repoName, fileName, fileLocation, fileLocationFilename, md5Hash, languageName, codeLines.size(), StringUtils.join(codeLines, " "), repoRemoteLocation, codeOwner));
                    }

                    fileLocations.add(fileLocationFilename);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            Singleton.getLogger().warning("ERROR - caught a " + ex.getClass() + " in " + this.getClass() +  "\n with message: " + ex.getMessage());
        }

        if (existingRepo) {
            CodeSearcher cs = new CodeSearcher();
            List<String> indexLocations = cs.getRepoDocuments(repoName);

            for (String file : indexLocations) {
                if (!fileLocations.contains(file)) {
                    Singleton.getLogger().info("Missing from disk, removing from index " + file);
                    try {
                        CodeIndexer.deleteByFileLocationFilename(file);
                    } catch (IOException ex) {
                        Singleton.getLogger().warning("ERROR - caught a " + ex.getClass() + " in " + this.getClass() +  "\n with message: " + ex.getMessage());
                    }
                }
            }
        }
    }

    public String getBlameFilePath(String fileLocationFilename) {
        String[] split = fileLocationFilename.split("/");
        String newString = String.join("/", Arrays.asList(split).subList(1, split.length));
        return newString;
    }


    /**
     * Only works if we have path to GIT
     */
    private List<CodeOwner> getBlameInfoExternal(int codeLinesSize, String repoName, String repoLocations, String fileName) {
        List<CodeOwner> codeOwners = new ArrayList<>(codeLinesSize);

        try {
            // -w is to ignore whitespace bug
            ProcessBuilder processBuilder = new ProcessBuilder(this.GITBINARYPATH, "blame", "-c", "-w", fileName);
            // The / part is required due to centos bug for version 1.1.1
            processBuilder.directory(new File(repoLocations + "/" + repoName));

            Process process = processBuilder.start();

            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            DateFormat df = new SimpleDateFormat("yyyy-mm-dd kk:mm:ss");

            HashMap<String, CodeOwner> owners = new HashMap<>();

            boolean foundSomething = false;

            while ((line = br.readLine()) != null) {
                Singleton.getLogger().info("Blame line " + repoName + fileName + ": " + line);
                String[] split = line.split("\t");

                if (split.length > 2 && split[1].length() != 0) {
                    foundSomething = true;
                    String author = split[1].substring(1);
                    int commitTime = (int) (System.currentTimeMillis() / 1000);
                    try {
                        commitTime = (int) (df.parse(split[2]).getTime() / 1000);
                    }
                    catch(ParseException ex) {
                        Singleton.getLogger().info("time parse expection for " + repoName + fileName);
                    }

                    if (owners.containsKey(author)) {
                        CodeOwner codeOwner = owners.get(author);
                        codeOwner.incrementLines();

                        int timestamp = codeOwner.getMostRecentUnixCommitTimestamp();

                        if (commitTime > timestamp) {
                            codeOwner.setMostRecentUnixCommitTimestamp(commitTime);
                        }
                        owners.put(author, codeOwner);
                    } else {
                        owners.put(author, new CodeOwner(author, 1, commitTime));
                    }
                }
            }

            if (foundSomething == false) {
                // External call for CentOS issue
                String[] split = fileName.split("/");

                if ( split.length != 1) {
                    codeOwners = getBlameInfoExternal(codeLinesSize, repoName, repoLocations, String.join("/", Arrays.asList(split).subList(1, split.length)));
                }

            } else {
                codeOwners = new ArrayList<>(owners.values());
            }

        } catch (IOException | StringIndexOutOfBoundsException ex) {
            Singleton.getLogger().info("getBlameInfoExternal repoloc: " + repoLocations + "/" + repoName);
            Singleton.getLogger().info("getBlameInfoExternal fileName: " + fileName);
            Singleton.getLogger().warning("ERROR - caught a " + ex.getClass() + " in " + this.getClass() + "\n with message: " + ex.getMessage());
        }

        return codeOwners;
    }

    // TODO this method appears to leak memory like crazy... need to resolve this
    private List<CodeOwner> getBlameInfo(int codeLinesSize, String repoName, String repoLocations, String fileName) {
        List<CodeOwner> codeOwners = new ArrayList<>(codeLinesSize);
        try {
            // The / part is required due to centos bug for version 1.1.1
            // This appears to be correct
            String repoLoc = repoLocations + "/" + repoName + "/.git";

            Repository localRepository = new FileRepository(new File(repoLoc));
            BlameCommand blamer = new BlameCommand(localRepository);

            ObjectId commitID = localRepository.resolve("HEAD");

            if (commitID == null) {
                Singleton.getLogger().info("getBlameInfo commitID is null for " + repoLoc + " " + fileName);
                return codeOwners;
            }

            BlameResult blame;

            // Somewhere in here appears to be wrong...
            blamer.setStartCommit(commitID);
            blamer.setFilePath(fileName);
            blame = blamer.call();

            // Hail mary attempt to solve issue on CentOS Attempt to set at all costs
            if(blame == null) { // This one appears to solve the issue so don't remove it
                String[] split = fileName.split("/");
                blamer.setStartCommit(commitID);
                if ( split.length != 1) {
                    blamer.setFilePath(String.join("/", Arrays.asList(split).subList(1, split.length)));
                }
                blame = blamer.call();
            }
            if(blame == null) {
                String[] split = fileName.split("/");
                blamer.setStartCommit(commitID);
                if ( split.length != 1) {
                    blamer.setFilePath("/" + String.join("/", Arrays.asList(split).subList(1, split.length)));
                }
                blame = blamer.call();
            }

            if (blame == null) {
                Singleton.getLogger().info("getBlameInfo blame is null for " + repoLoc + " " + fileName);
            }


            if (blame != null) {
                // Get all the owners their number of commits and most recent commit
                HashMap<String, CodeOwner> owners = new HashMap<>();
                RevCommit commit;
                PersonIdent authorIdent;

                try {
                    for (int i = 0; i < codeLinesSize; i++) {
                        commit = blame.getSourceCommit(i);
                        authorIdent = commit.getAuthorIdent();

                        if (owners.containsKey(authorIdent.getName())) {
                            CodeOwner codeOwner = owners.get(authorIdent.getName());
                            codeOwner.incrementLines();

                            int timestamp = codeOwner.getMostRecentUnixCommitTimestamp();

                            if (commit.getCommitTime() > timestamp) {
                                codeOwner.setMostRecentUnixCommitTimestamp(commit.getCommitTime());
                            }
                            owners.put(authorIdent.getName(), codeOwner);
                        } else {
                            owners.put(authorIdent.getName(), new CodeOwner(authorIdent.getName(), 1, commit.getCommitTime()));
                        }
                    }
                }
                catch(IndexOutOfBoundsException ex) {
                    // Ignore this as its not really a problem or is it?
                    Singleton.getLogger().info("IndexOutOfBoundsException when trying to get blame for " + repoName + fileName);
                }

                codeOwners = new ArrayList<>(owners.values());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        System.gc(); // Try to clean up
        return codeOwners;
    }


    private RepositoryChanged updateGitRepository(String repoName, String repoRemoteLocation, String repoUserName, String repoPassword, String repoLocations, String branch, boolean useCredentials) {
        boolean changed = false;
        List<String> changedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();
        Singleton.getLogger().info("Attempting to pull latest from " + repoRemoteLocation + " for " + repoName);

        try {
            Repository localRepository = new FileRepository(new File(repoLocations + "/" + repoName + "/.git"));

            Ref head = localRepository.getRef("HEAD");

            Git git = new Git(localRepository);
            git.reset();
            git.clean();

            PullCommand pullCmd = git.pull();

            if(useCredentials) {
                pullCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(repoUserName, repoPassword));
            }

            pullCmd.call();
            Ref newHEAD = localRepository.getRef("HEAD");

            if(!head.toString().equals(newHEAD.toString())) {
                changed = true;

                // Get the differences between the the heads which we updated at
                // and use these to just update the differences between them
                ObjectId oldHead = localRepository.resolve(head.getObjectId().getName() + "^{tree}");
                ObjectId newHead = localRepository.resolve(newHEAD.getObjectId().getName() + "^{tree}");

                ObjectReader reader = localRepository.newObjectReader();

                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset(reader, oldHead);

                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset(reader, newHead);


                List<DiffEntry> entries = git.diff()
                                            .setNewTree(newTreeIter)
                                            .setOldTree(oldTreeIter)
                                            .call();


                for( DiffEntry entry : entries ) {
                    if ("DELETE".equals(entry.getChangeType().name())) {
                        deletedFiles.add(FilenameUtils.separatorsToUnix(entry.getOldPath()));
                    }
                    else {
                        changedFiles.add(FilenameUtils.separatorsToUnix(entry.getNewPath()));
                    }
                }
            }

        } catch (IOException | GitAPIException | InvalidPathException ex) {
            changed = false;
            Singleton.getLogger().warning("ERROR - caught a " + ex.getClass() + " in " + this.getClass() +  "\n with message: " + ex.getMessage());
        }

        return new RepositoryChanged(changed, changedFiles, deletedFiles);
    }


    private RepositoryChanged cloneGitRepository(String repoName, String repoRemoteLocation, String repoUserName, String repoPassword, String repoLocations, String branch, boolean useCredentials) {
        boolean successful = false;
        Singleton.getLogger().info("Attempting to clone " + repoRemoteLocation);

        try {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setURI(repoRemoteLocation);
            cloneCommand.setDirectory(new File(repoLocations + "/" + repoName + "/"));
            cloneCommand.setCloneAllBranches(true);
            cloneCommand.setBranch(branch);

            if(useCredentials) {
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(repoUserName, repoPassword));
            }

            cloneCommand.call();

            successful = true;

        } catch (GitAPIException | InvalidPathException ex) {
            successful = false;
            Singleton.getLogger().warning("ERROR - caught a " + ex.getClass() + " in " + this.getClass() +  "\n with message: " + ex.getMessage());
        }

        RepositoryChanged repositoryChanged = new RepositoryChanged(successful);
        repositoryChanged.setClone(true);

        return repositoryChanged;
    }

    // The below are used to create temp files indicating everything worked

    public void createCloneUpdateSuccess(String repoLocation) {
        createFile(repoLocation, "cloneupdate");
    }

    public void deleteCloneUpdateSuccess(String repoLocation) {
        deleteFile(repoLocation, "cloneupdate");
    }

    public boolean checkCloneUpdateSucess(String repoLocation) {
        return checkFile(repoLocation, "cloneupdate");
    }

    public void createIndexSuccess(String repoLocation) {
        createFile(repoLocation, "index");
    }

    public void deleteIndexSuccess(String repoLocation) {
        deleteFile(repoLocation, "index");
    }

    public boolean checkIndexSucess(String repoLocation) {
        return checkFile(repoLocation, "index");
    }

    private boolean checkFile(String repoLocation, String type) {
        File f = new File(repoLocation + "/searchcode." + type + ".success");
        return f.exists();
    }

    private void createFile(String repoLocation, String type) {
        File file = new File(repoLocation, "/searchcode." + type + ".success");
        file.mkdir();
    }

    private void deleteFile(String repoLocation, String type) {
        File file = new File(repoLocation, "/searchcode." + type + ".success");

        file.delete();
    }
}
