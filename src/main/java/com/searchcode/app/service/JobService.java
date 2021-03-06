/*
 * Copyright (c) 2016 Boyter Online Services
 *
 * Use of this software is governed by the Fair Source License included
 * in the LICENSE.TXT file
 */

package com.searchcode.app.service;

import com.google.inject.Inject;
import com.searchcode.app.config.Values;
import com.searchcode.app.dao.IRepo;
import com.searchcode.app.jobs.*;
import com.searchcode.app.model.RepoResult;
import com.searchcode.app.util.Properties;
import org.quartz.*;

import java.util.List;
import java.util.logging.Logger;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Starts all of the quartz jobs which perform background tasks such as cloning/updating from GIT/SVN and
 * the jobs which delete repositories and which add repositories to the queue to be indexed.
 */
public class JobService implements IJobService {

    private static final Logger LOGGER = Singleton.getLogger();

    private IRepo repo = null;
    private int UPDATETIME = 600;
    private int INDEXTIME = 10; // TODO allow this to be configurable
    private int NUMBERPROCESSORS = 5; // TODO allow this to be configurable

    private String REPOLOCATION = Properties.getProperties().getProperty(Values.REPOSITORYLOCATION, Values.DEFAULTREPOSITORYLOCATION);
    private boolean LOWMEMORY = Boolean.parseBoolean(com.searchcode.app.util.Properties.getProperties().getProperty(Values.LOWMEMORY, Values.DEFAULTLOWMEMORY));
    private boolean SVNENABLED = Boolean.parseBoolean(com.searchcode.app.util.Properties.getProperties().getProperty(Values.SVNENABLED, Values.DEFAULTSVNENABLED));

    @Inject
    public JobService(IRepo repo) {
        this.repo = repo;
        try {
            this.UPDATETIME = Integer.parseInt(Properties.getProperties().getProperty(Values.CHECKREPOCHANGES, "600"));
        }
        catch(NumberFormatException ex) {
            this.UPDATETIME = 600;
        }
    }

    /**
     * Creates a git repo indexer job which will pull from the list of git repositories and start
     * indexing them
     */
    public void startIndexGitRepoJobs(String uniquename) {
        try {
            Scheduler scheduler = Singleton.getScheduler();


            JobDetail job = newJob(IndexGitRepoJob.class)
                    .withIdentity("updateindex-git-" + uniquename)
                    .build();

            SimpleTrigger trigger = newTrigger()
                    .withIdentity("updateindex-git-" + uniquename)
                    .withSchedule(simpleSchedule()
                                    .withIntervalInSeconds(this.INDEXTIME)
                                    .repeatForever()
                    )
                    .build();

            job.getJobDataMap().put("REPOLOCATIONS", this.REPOLOCATION);
            job.getJobDataMap().put("LOWMEMORY", this.LOWMEMORY);

            scheduler.scheduleJob(job, trigger);

            scheduler.start();
        }
        catch(SchedulerException ex) {
            LOGGER.severe(" caught a " + ex.getClass() + "\n with message: " + ex.getMessage());
        }
    }

    /**
     * Creates a svn repo indexer job which will pull from the list of git repositories and start
     * indexing them
     */
    public void startIndexSvnRepoJobs(String uniquename) {
        try {
            Scheduler scheduler = Singleton.getScheduler();


            JobDetail job = newJob(IndexSvnRepoJob.class)
                    .withIdentity("updateindex-svn-" + uniquename)
                    .build();

            SimpleTrigger trigger = newTrigger()
                    .withIdentity("updateindex-svn-" + uniquename)
                    .withSchedule(simpleSchedule()
                                    .withIntervalInSeconds(this.INDEXTIME)
                                    .repeatForever()
                    )
                    .build();

            job.getJobDataMap().put("REPOLOCATIONS", this.REPOLOCATION);
            job.getJobDataMap().put("LOWMEMORY", this.LOWMEMORY);

            scheduler.scheduleJob(job, trigger);

            scheduler.start();
        }
        catch(SchedulerException ex) {
            LOGGER.severe(" caught a " + ex.getClass() + "\n with message: " + ex.getMessage());
        }
    }

    /**
     * Starts a background job which pulls all repositories from the database and adds them to the
     * queue to be indexed
     */
    public void startEnqueueJob() {
        try {
            Scheduler scheduler = Singleton.getScheduler();

            // Setup the indexer which runs forever adding documents to be indexed
            JobDetail job = newJob(EnqueueRepositoryJob.class)
                    .withIdentity("enqueuejob")
                    .build();

            SimpleTrigger trigger = newTrigger()
                    .withIdentity("enqueuejob")
                    .withSchedule(simpleSchedule()
                                    .withIntervalInSeconds(this.UPDATETIME)
                                    .repeatForever()
                    )
                    .build();

            scheduler.scheduleJob(job, trigger);
            scheduler.start();
        }  catch(SchedulerException ex) {
            LOGGER.severe(" caught a " + ex.getClass() + "\n with message: " + ex.getMessage());
        }
    }

    /**
     * Starts a background job which deletes repositories from the database, index and checked out disk
     */
    public void startDeleteJob() {
        try {
            Scheduler scheduler = Singleton.getScheduler();

            // Setup the indexer which runs forever adding documents to be indexed
            JobDetail job = newJob(DeleteRepositoryJob.class)
                    .withIdentity("deletejob")
                    .build();

            SimpleTrigger trigger = newTrigger()
                    .withIdentity("deletejob")
                    .withSchedule(simpleSchedule()
                                    .withIntervalInSeconds(1)
                                    .repeatForever()
                    )
                    .build();

            scheduler.scheduleJob(job, trigger);
            scheduler.start();
        }  catch(SchedulerException ex) {
            LOGGER.severe(" caught a " + ex.getClass() + "\n with message: " + ex.getMessage());
        }
    }

    /**
     * Starts all of the above jobs as per their unique requirements
     * TODO fix so this can only run once
     * TODO move the indexer job start into method like the above ones
     */
    public void initialJobs() {
        try {
            Scheduler scheduler = Singleton.getScheduler();

            List<RepoResult> repoResults = this.repo.getAllRepo();

            // Create a pool of crawlers which read from the queue
            for(int i=0; i< this.NUMBERPROCESSORS; i++) {
                this.startIndexGitRepoJobs("" + i);
                if (SVNENABLED) {
                    this.startIndexSvnRepoJobs("" + i);
                }
            }

            if(repoResults.size() == 0) {
                LOGGER.info("///////////////////////////////////////////////////////////////////////////\n      // You have no repositories set to index. Add some using the admin page. //\n      // Browse to the admin page and manually add some repositories to index. //\n      ///////////////////////////////////////////////////////////////////////////");
                System.out.println("///////////////////////////////////////////////////////////////////////////\n      // You have no repositories set to index. Add some using the admin page. //\n      // Browse to the admin page and manually add some repositories to index. //\n      ///////////////////////////////////////////////////////////////////////////");
            }

            // Setup the job which queues things to be downloaded and then indexed
            startEnqueueJob();
            // Setup the job which deletes repositories
            startDeleteJob();

            // Setup the indexer which runs forever indexing
            JobDetail job = newJob(IndexDocumentsJob.class)
                    .withIdentity("indexerjob")
                    .build();

            SimpleTrigger trigger = newTrigger()
                    .withIdentity("indexerjob")
                    .withSchedule(simpleSchedule()
                                    .withIntervalInSeconds(this.INDEXTIME)
                                    .repeatForever()
                    )
                    .build();

            scheduler.scheduleJob(job, trigger);
            scheduler.start();
        } catch(SchedulerException ex) {
            LOGGER.severe(" caught a " + ex.getClass() + "\n with message: " + ex.getMessage());
        }
    }
}
