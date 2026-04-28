package com.easyrail.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApiSessionHandlerTest {

    @Test
    public void recognizesUnauthorizedStatuses() {
        assertTrue(ApiSessionHandler.isUnauthorizedStatus(401));
        assertTrue(ApiSessionHandler.isUnauthorizedStatus(403));
        assertFalse(ApiSessionHandler.isUnauthorizedStatus(400));
        assertFalse(ApiSessionHandler.isUnauthorizedStatus(500));
    }
}
