/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.cluster.protocol.jaxb.message;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.nifi.cluster.protocol.NodeIdentifier;
import org.apache.nifi.cluster.protocol.StandardDataFlow;

/**
 */
public class AdaptedConnectionResponse {

    private StandardDataFlow dataFlow;
    private NodeIdentifier nodeIdentifier;
    private String rejectionReason;
    private int tryLaterSeconds;
    private Integer managerRemoteInputPort;
    private Integer managerRemoteInputHttpPort;
    private Boolean managerRemoteCommsSecure;
    private String instanceId;

    public AdaptedConnectionResponse() {
    }

    @XmlJavaTypeAdapter(DataFlowAdapter.class)
    public StandardDataFlow getDataFlow() {
        return dataFlow;
    }

    public void setDataFlow(StandardDataFlow dataFlow) {
        this.dataFlow = dataFlow;
    }

    @XmlJavaTypeAdapter(NodeIdentifierAdapter.class)
    public NodeIdentifier getNodeIdentifier() {
        return nodeIdentifier;
    }

    public void setNodeIdentifier(NodeIdentifier nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    public int getTryLaterSeconds() {
        return tryLaterSeconds;
    }

    public void setTryLaterSeconds(int tryLaterSeconds) {
        this.tryLaterSeconds = tryLaterSeconds;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(final String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean shouldTryLater() {
        return tryLaterSeconds > 0;
    }

    public void setManagerRemoteInputPort(Integer managerRemoteInputPort) {
        this.managerRemoteInputPort = managerRemoteInputPort;
    }

    public Integer getManagerRemoteInputPort() {
        return managerRemoteInputPort;
    }

    public void setManagerRemoteInputHttpPort(Integer managerRemoteInputHttpPort) {
        this.managerRemoteInputHttpPort = managerRemoteInputHttpPort;
    }

    public Integer getManagerRemoteInputHttpPort() {
        return managerRemoteInputHttpPort;
    }

    public void setManagerRemoteCommsSecure(Boolean secure) {
        this.managerRemoteCommsSecure = secure;
    }

    public Boolean isManagerRemoteCommsSecure() {
        return managerRemoteCommsSecure;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
