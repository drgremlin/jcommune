/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jtalks.jcommune.service.transactional;

import org.jtalks.common.model.permissions.GeneralPermission;
import org.jtalks.common.security.SecurityService;
import org.jtalks.common.service.security.SecurityContextFacade;
import org.jtalks.jcommune.model.dao.BranchDao;
import org.jtalks.jcommune.model.dao.TopicDao;
import org.jtalks.jcommune.model.entity.Branch;
import org.jtalks.jcommune.model.entity.JCUser;
import org.jtalks.jcommune.model.entity.Poll;
import org.jtalks.jcommune.model.entity.Post;
import org.jtalks.jcommune.model.entity.Topic;
import org.jtalks.jcommune.service.*;
import org.jtalks.jcommune.service.exceptions.NotFoundException;
import org.jtalks.jcommune.service.nontransactional.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

/**
 * Topic service class. This class contains method needed to manipulate with Topic persistent entity.
 *
 * @author Osadchuck Eugeny
 * @author Vervenko Pavel
 * @author Kirill Afonin
 * @author Vitaliy Kravchenko
 * @author Max Malakhov
 * @author Eugeny Batov
 */
public class TransactionalTopicModificationService implements TopicModificationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TopicDao dao;

    private TopicFetchService topicFetchService;
    private SecurityService securityService;
    private BranchDao branchDao;
    private NotificationService notificationService;
    private SubscriptionService subscriptionService;
    private UserService userService;
    private PollService pollService;
    private PermissionEvaluator permissionEvaluator;
    private SecurityContextFacade securityContextFacade;

    /**
     * Create an instance of User entity based service
     *
     * @param dao                   data access object, which should be able do all CRUD operations with topic entity
     * @param securityService       {@link org.jtalks.common.security.SecurityService} for retrieving current user
     * @param branchDao             used for checking branch existence
     * @param notificationService   to send email notifications on topic updates to subscribed users
     * @param subscriptionService   for subscribing user on topic if notification enabled
     * @param userService           to get current logged in user
     * @param pollService           to create a poll and vote in a poll
     * @param topicFetchService     to retrieve topics from a database
     * @param securityContextFacade authentication object retrieval
     * @param permissionEvaluator   for authorization purposes
     */
    public TransactionalTopicModificationService(TopicDao dao, SecurityService securityService,
                                                 BranchDao branchDao,
                                                 NotificationService notificationService,
                                                 SubscriptionService subscriptionService,
                                                 UserService userService,
                                                 PollService pollService,
                                                 TopicFetchService topicFetchService,
                                                 SecurityContextFacade securityContextFacade,
                                                 PermissionEvaluator permissionEvaluator) {
        this.dao = dao;
        this.securityService = securityService;
        this.branchDao = branchDao;
        this.notificationService = notificationService;
        this.subscriptionService = subscriptionService;
        this.userService = userService;
        this.pollService = pollService;
        this.topicFetchService = topicFetchService;
        this.securityContextFacade = securityContextFacade;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasPermission(#branchId, 'BRANCH', 'BranchPermission.CREATE_POSTS')")
    public Post replyToTopic(long topicId, String answerBody, long branchId) throws NotFoundException {
        Topic topic = topicFetchService.get(topicId);
        this.assertPostingIsAllowed(topic);

        JCUser currentUser = userService.getCurrentUser();
        currentUser.setPostCount(currentUser.getPostCount() + 1);

        Post answer = new Post(currentUser, answerBody);
        topic.addPost(answer);
        dao.update(topic);

        securityService.createAclBuilder().grant(GeneralPermission.WRITE).to(currentUser).on(answer).flush();
        notificationService.topicChanged(topic);
        logger.debug("New post in topic. Topic id={}, Post id={}, Post author={}",
                new Object[]{topicId, answer.getId(), currentUser.getUsername()});

        return answer;
    }

    /**
     * Checks if the current topic is closed for posting.
     * Some users, however, can add posts even to the closed branches. These
     * users are granted with BranchPermission.CLOSE_TOPICS permission.
     *
     * @param topic topic to be checked for if posting is allowed
     */
    private void assertPostingIsAllowed(Topic topic) {
        Authentication auth = securityContextFacade.getContext().getAuthentication();
        if (topic.isClosed() && !permissionEvaluator.hasPermission(
                auth, topic.getBranch().getId(), "BRANCH", "BranchPermission.CLOSE_TOPICS")) { // holy shit...
            throw new AccessDeniedException("Posting is forbidden for closed topics");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasPermission(#topicDto.branch.id, 'BRANCH', 'BranchPermission.CREATE_TOPICS')")
    public Topic createTopic(Topic topicDto, String bodyText,
                             boolean notifyOnAnswers) throws NotFoundException {
        JCUser currentUser = userService.getCurrentUser();

        currentUser.setPostCount(currentUser.getPostCount() + 1);
        Topic topic = new Topic(currentUser, topicDto.getTitle());
        Post first = new Post(currentUser, bodyText);
        topic.addPost(first);
        Branch branch = topicDto.getBranch();

        branch.addTopic(topic);
        branchDao.update(branch);

        JCUser user = userService.getCurrentUser();
        securityService.createAclBuilder().grant(GeneralPermission.WRITE).to(user).on(topic).flush();
        securityService.createAclBuilder().grant(GeneralPermission.WRITE).to(user).on(first).flush();

        notificationService.branchChanged(branch);

        subscribeOnTopicIfNotificationsEnabled(notifyOnAnswers, topic, currentUser);

        Poll poll = topicDto.getPoll();
        if (poll != null && poll.isHasPoll()) {
            poll.setTopic(topic);
            pollService.createPoll(poll);
        }

        logger.debug("Created new topic id={}, branch id={}, author={}",
                new Object[]{topic.getId(), branch.getId(), currentUser.getUsername()});
        return topic;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasPermission(#topic.id, 'TOPIC', 'GeneralPermission.WRITE') and " +
            "hasPermission(#topic.branch.id, 'BRANCH', 'BranchPermission.EDIT_OWN_POSTS') or " +
            "hasPermission(#topic.branch.id, 'BRANCH', 'BranchPermission.EDIT_OTHERS_POSTS')")
    public void updateTopic(Topic topic, boolean notifyOnAnswers){
        Post post = topic.getFirstPost();
        post.updateModificationDate();
        dao.update(topic);
        notificationService.topicChanged(topic);
        JCUser currentUser = userService.getCurrentUser();
        subscribeOnTopicIfNotificationsEnabled(notifyOnAnswers, topic, currentUser);
        logger.debug("Topic id={} updated", topic.getId());
    }

    /**
     * Subscribes topic starter on created topic if notifications enabled("Notify me about the answer" checkbox).
     *
     * @param notifyOnAnswers flag that indicates notifications state(enabled or disabled)
     * @param topic           topic to subscription
     * @param currentUser     current user
     */
    private void subscribeOnTopicIfNotificationsEnabled(boolean notifyOnAnswers, Topic topic, JCUser currentUser) {
        boolean subscribed = topic.userSubscribed(currentUser);
        if (notifyOnAnswers ^ subscribed) {
            subscriptionService.toggleTopicSubscription(topic);
        }
    }

    /**
     * {@inheritDoc}
     */
    @PreAuthorize("hasPermission(#topic.branch.id, 'BRANCH', 'BranchPermission.DELETE_TOPICS')")
    @Override
    public void deleteTopic(Topic topic) throws NotFoundException {
        Branch branch = deleteTopicSilent(topic);
        notificationService.branchChanged(branch);

        logger.info("Deleted topic \"{}\". Topic id: {}", topic.getTitle(), topic.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTopicSilent(long topicId) throws NotFoundException {
        Topic topic = topicFetchService.get(topicId);
        this.deleteTopicSilent(topic);
    }

    /**
     * Performs actual topic deletion. Deletes all topic related data and
     * recalculates user's post count.
     *
     * @param topic topic to delete
     * @return branch without deleted topic
     */
    private Branch deleteTopicSilent(Topic topic) {
        for (Post post : topic.getPosts()) {
            JCUser user = post.getUserCreated();
            user.setPostCount(user.getPostCount() - 1);
        }

        Branch branch = topic.getBranch();
        branch.deleteTopic(topic);
        branchDao.update(branch);

        securityService.deleteFromAcl(Topic.class, topic.getId());
        return branch;
    }

    /**
     * {@inheritDoc}
     */
    @PreAuthorize("hasPermission(#topic.branch.id, 'BRANCH', 'BranchPermission.MOVE_TOPICS')")
    @Override
    public void moveTopic(Topic topic, Long branchId) throws NotFoundException {
        Branch targetBranch = branchDao.get(branchId);
        targetBranch.addTopic(topic);
        branchDao.update(targetBranch);

        notificationService.topicMoved(topic, topic.getId());

        logger.info("Moved topic \"{}\". Topic id: {}", topic.getTitle(), topic.getId());
    }

    /**
     * {@inheritDoc}
     */
    @PreAuthorize("hasPermission(#topic.branch.id, 'BRANCH', 'BranchPermission.CLOSE_TOPICS')")
    @Override
    public void closeTopic(Topic topic) {
        topic.setClosed(true);
        dao.update(topic);
    }

    /**
     * {@inheritDoc}
     */
    @PreAuthorize("hasPermission(#topic.branch.id, 'BRANCH', 'BranchPermission.CLOSE_TOPICS')")
    @Override
    public void openTopic(Topic topic) {
        topic.setClosed(false);
        dao.update(topic);
    }
}
