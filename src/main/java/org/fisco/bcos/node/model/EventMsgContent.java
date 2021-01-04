package org.fisco.bcos.node.model;

import java.util.List;

public class EventMsgContent {
    private String fromBlock;
    private String toBlock;
    private List<String> addresses;
    private List<Object> topics;
    private String groupID;
    private String filterID;

    public String getFilterID() {
        return filterID;
    }
}
