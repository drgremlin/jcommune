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
package org.jtalks.jcommune.model.dto;

import org.jtalks.jcommune.model.entity.JCUser;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class PageRequestTest {

    private static final int PAGE_SIZE_10 = 10;
    private static final String PAGE_NUMBER = "5";
    public static final int PARSED_PAGE_NUMBER = Integer.parseInt(PAGE_NUMBER);
    private static final int INDEX_OF_FIRST_ITEM = (PARSED_PAGE_NUMBER - 1) * PAGE_SIZE_10;
    public static final int ZERO = 0;
    public static final String REQUESTED_ZERO_PAGE_NUMBER = "0";
    public static final String REQUESTED_NEGATIVE_PAGE_NUMBER = "-1";
    public static final int TOTAL_COUNT_10 = 10;
    public static final int TOTAL_COUNT_20 = 20;
    public static final String REQUESTED_PAGE_NUMBER_1000 = "1000";
    public static final int RETURNED_PAGE_2 = 2;
    public static final String GARBAGE = "garbage";
    public static final int TOTAL_COUNT_50 = 50;

    private PageRequest pageRequest;
    
    @Test
    public void testConstructorForValidArguments() {
        pageRequest = new PageRequest(PAGE_NUMBER, PAGE_SIZE_10);
        assertEquals(pageRequest.getPageNumber(), PARSED_PAGE_NUMBER);
        assertEquals(pageRequest.getPageSize(), PAGE_SIZE_10);
        assertEquals(pageRequest.getOffset(), INDEX_OF_FIRST_ITEM);
        assertEquals(pageRequest.getSort(), null);
    }
    
    @Test
    public void testConstructorLessThanOnePageNumberConvertedToFirstPageNumber() {
        pageRequest = new PageRequest(REQUESTED_ZERO_PAGE_NUMBER, PAGE_SIZE_10);
        assertEquals(pageRequest.getPageNumber(), PageRequest.FIRST_PAGE_NUMBER);
    }

    @Test()
    public void testConstructorLessThanOnePageSizeConvertedToDefaultPageSize() {
        pageRequest = new PageRequest(PAGE_NUMBER, ZERO);
        assertEquals(pageRequest.getPageSize(), JCUser.DEFAULT_PAGE_SIZE);
    }

    @Test()
    public void testConstructorNonNumericPageSizeConvertedToDefaultPageSize() {
        pageRequest = new PageRequest(GARBAGE, ZERO);
        assertEquals(pageRequest.getPageSize(), JCUser.DEFAULT_PAGE_SIZE);
    }

    @Test
    public void testGetOffsetLessThanOnePageNumber() {
        pageRequest = new PageRequest(REQUESTED_NEGATIVE_PAGE_NUMBER, PAGE_SIZE_10);
        assertEquals(pageRequest.getOffset(), ZERO);
    }

    @Test
    public void testAdjustPageNumber() {
        pageRequest = new PageRequest(PAGE_NUMBER, PAGE_SIZE_10);
        pageRequest.adjustPageNumber(TOTAL_COUNT_50);
        int actualPageNumber = pageRequest.getPageNumber();
        int expectedPageNumber = Integer.parseInt(PAGE_NUMBER);
        assertEquals(actualPageNumber, expectedPageNumber);
    }
    @Test
    public void testAdjustPageNumberForLessThanOnePageNumberReturnedFirstPage() {
        pageRequest = new PageRequest(REQUESTED_ZERO_PAGE_NUMBER, PAGE_SIZE_10);
        pageRequest.adjustPageNumber(TOTAL_COUNT_10);
        assertEquals(pageRequest.getPageNumber(), PageRequest.FIRST_PAGE_NUMBER);
    }

    @Test
    public void testAdjustPageNumberForTooBigPageNumberReturnedLastPage() {
        pageRequest = new PageRequest(REQUESTED_PAGE_NUMBER_1000, PAGE_SIZE_10);
        pageRequest.adjustPageNumber(TOTAL_COUNT_20);
        assertEquals(pageRequest.getPageNumber(), RETURNED_PAGE_2);
    }
    
    

}
