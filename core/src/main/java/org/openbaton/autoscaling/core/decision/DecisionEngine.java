/*
 *
 *  *
 *  * Copyright (c) 2015 Technische Universität Berlin
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.openbaton.autoscaling.core.decision;

import org.openbaton.autoscaling.configuration.NfvoProperties;
import org.openbaton.autoscaling.core.execution.ExecutionManagement;
import org.openbaton.catalogue.mano.common.ScalingAction;
import org.openbaton.catalogue.mano.record.NetworkServiceRecord;
import org.openbaton.catalogue.mano.record.Status;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.sdk.NFVORequestor;
import org.openbaton.sdk.api.exception.SDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * Created by mpa on 27.10.15.
 */
@Service
@Scope("singleton")
public class DecisionEngine {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    //@Autowired
    private ExecutionManagement executionManagement;

    private NFVORequestor nfvoRequestor;

    @Autowired
    private NfvoProperties nfvoProperties;

    @PostConstruct
    public void init() {
        this.executionManagement = context.getBean(ExecutionManagement.class);
        this.nfvoRequestor = new NFVORequestor(nfvoProperties.getUsername(), nfvoProperties.getPassword(), nfvoProperties.getIp(), nfvoProperties.getPort(), "1");
    }

    public void sendDecision(String nsr_id, String vnfr_id, Set<ScalingAction> actions, long cooldown) {
        //log.info("[DECISION_MAKER] DECIDED_ABOUT_ACTIONS " + new Date().getTime());
        log.debug("Send actions to Executor: " + actions.toString());
        executionManagement.executeActions(nsr_id, vnfr_id, actions, cooldown);
    }

    public Status getStatus(String nsr_id, String vnfr_id) {
        log.debug("Check Status of VNFR with id: " + vnfr_id);
        NetworkServiceRecord networkServiceRecord = null;
        try {
            networkServiceRecord = nfvoRequestor.getNetworkServiceRecordAgent().findById(nsr_id);
        } catch (SDKException e) {
            log.warn(e.getMessage(), e);
            return Status.NULL;
        } catch (ClassNotFoundException e) {
            log.warn(e.getMessage(), e);
            return Status.NULL;
        }
        if (networkServiceRecord == null || networkServiceRecord.getStatus() == null) {
            return Status.NULL;
        }
        return networkServiceRecord.getStatus();
    }

    public VirtualNetworkFunctionRecord getVNFR(String nsr_id, String vnfr_id) throws SDKException {
        try {
            VirtualNetworkFunctionRecord vnfr = nfvoRequestor.getNetworkServiceRecordAgent().getVirtualNetworkFunctionRecord(nsr_id, vnfr_id);
            return vnfr;
        } catch (SDKException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

}
