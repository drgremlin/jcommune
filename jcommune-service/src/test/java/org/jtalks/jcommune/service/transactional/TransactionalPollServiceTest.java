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

import org.joda.time.DateTime;
import org.jtalks.common.model.dao.ChildRepository;
import org.jtalks.jcommune.model.entity.Poll;
import org.jtalks.jcommune.model.entity.PollOption;
import org.jtalks.jcommune.service.PollService;
import org.jtalks.jcommune.service.nontransactional.SecurityService;
import org.jtalks.jcommune.service.security.AclBuilder;
import org.jtalks.jcommune.service.security.TemporaryAuthorityManager;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Anuar Nurmakanov
 */
public class TransactionalPollServiceTest {
    private static final int VOTES_COUNT = 4;
    private static final Long POLL_ID = 1L;
    private PollService pollService;
    @Mock
    private ChildRepository<PollOption> pollOptionDao;
    @Mock
    private ChildRepository<Poll> pollDao;
    @Mock
    private SecurityService securityService;
    @Mock
    private AclBuilder aclBuilder;
    @Mock
    private TemporaryAuthorityManager temporaryAuthorityManager;
    

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
        pollService = new TransactionalPollService(pollDao, pollOptionDao,
                securityService, temporaryAuthorityManager);

        Mockito.when(aclBuilder.write()).thenReturn(aclBuilder);
        Mockito.when(aclBuilder.on(Mockito.any(Poll.class))).thenReturn(aclBuilder);
        Mockito.when(securityService.grantToCurrentUser()).thenReturn(aclBuilder);

    }

    @Test
    public void testAddSingleVote() {
        List<Long> pollOptionIds = Arrays.asList(1L);
        Poll poll = createPollWithOptions(POLL_ID, pollOptionIds, VOTES_COUNT, null);

        Mockito.when(pollDao.get(POLL_ID)).thenReturn(poll);

        Poll resultPoll = pollService.vote(POLL_ID, pollOptionIds);
        PollOption resultPollOption = resultPoll.getPollOptions().get(0);

        Assert.assertEquals(resultPollOption.getVotesCount(), VOTES_COUNT + 1,
                "Count of votes should be increased.");
    }

    @Test
    public void testAddSingleVoteInInactivePoll() {
        List<Long> pollOptionIds = Arrays.asList(1L);
        DateTime endingDate = new DateTime(1999, 1, 1, 1, 1, 1, 1);
        Poll poll = createPollWithOptions(POLL_ID, pollOptionIds, VOTES_COUNT, endingDate);

        Mockito.when(pollDao.get(POLL_ID)).thenReturn(poll);

        Poll resultPoll = pollService.vote(POLL_ID, pollOptionIds);
        PollOption resultPollOption = resultPoll.getPollOptions().get(0);

        Assert.assertEquals(resultPollOption.getVotesCount(), VOTES_COUNT,
                "Count of votes should be increased.");
    }

    @Test
    public void testAddMultipleVotes() {
        List<Long> pollOptionIds = Arrays.asList(1L, 5L, 9L);
        Poll poll = createPollWithOptions(POLL_ID, pollOptionIds, VOTES_COUNT, null);

        Mockito.when(pollDao.get(Mockito.anyLong())).thenReturn(poll);

        Poll resultPoll = pollService.vote(POLL_ID, pollOptionIds);

        for (PollOption option : resultPoll.getPollOptions()) {
            Assert.assertEquals(option.getVotesCount(), VOTES_COUNT + 1,
                    "Count of votes should be increased.");
        }
    }

    @Test
    public void testAddMultipleVotesInInactivePoll() {
        List<Long> pollOptionIds = Arrays.asList(1L, 5L, 9L);
        DateTime endingDate = new DateTime(1999, 1, 1, 1, 1, 1, 1);
        Poll poll = createPollWithOptions(POLL_ID, pollOptionIds, VOTES_COUNT, endingDate);

        Mockito.when(pollDao.get(Mockito.anyLong())).thenReturn(poll);

        Poll resultPoll = pollService.vote(POLL_ID, pollOptionIds);

        for (PollOption option : resultPoll.getPollOptions()) {
            Assert.assertEquals(option.getVotesCount(), VOTES_COUNT,
                    "Count of votes should be increased.");
        }
    }
    
    private Poll createPollWithOptions(Long pollId, List<Long> pollOptionIds,
            int initialVoteCount, DateTime endingDate) {
        Poll poll = new Poll("Poll");
        poll.setEndingDate(endingDate);
        poll.setId(pollId);
        for (Long id : pollOptionIds) {
            PollOption option = new PollOption("Option:" + String.valueOf(id));
            option.setId(id);
            option.setVotesCount(initialVoteCount);
            poll.addPollOptions(option);
        }
        return poll;
    }
}
