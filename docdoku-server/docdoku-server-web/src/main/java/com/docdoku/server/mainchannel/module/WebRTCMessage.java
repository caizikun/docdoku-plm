/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2013 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.docdoku.server.mainchannel.module;



public class WebRTCMessage extends AbstractMessage{

    public static final String WEBRTC_OFFLINE = "OFFLINE";

    private String roomKey;
    private String reason;
    private String context;
    private int roomOccupancy;
    private String userLogin;

    public WebRTCMessage(){

    }

    public WebRTCMessage(String type, String remoteUser){
        super(type,remoteUser);
    }
    public WebRTCMessage(String type, String remoteUser, String roomKey, String reason, String context, int roomOccupancy, String userLogin) {
        super(type,remoteUser);
        this.roomKey = roomKey;
        this.reason = reason;
        this.context = context;
        this.roomOccupancy = roomOccupancy;
        this.userLogin = userLogin;
    }


    public String getRoomKey() {
        return roomKey;
    }

    public void setRoomKey(String roomKey) {
        this.roomKey = roomKey;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public int getRoomOccupancy() {
        return roomOccupancy;
    }

    public void setRoomOccupancy(int roomOccupancy) {
        this.roomOccupancy = roomOccupancy;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }
}
