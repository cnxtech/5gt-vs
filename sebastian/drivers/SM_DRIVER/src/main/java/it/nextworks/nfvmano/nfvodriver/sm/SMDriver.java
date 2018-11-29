/*
* Copyright 2018 ATOS.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package it.nextworks.nfvmano.nfvodriver.sm;


import org.springframework.web.client.RestTemplate;

import it.nextworks.nfvmano.libs.catalogues.interfaces.MecAppPackageManagementConsumerInterface;
import it.nextworks.nfvmano.libs.catalogues.interfaces.NsdManagementConsumerInterface;
import it.nextworks.nfvmano.libs.catalogues.interfaces.VnfPackageManagementConsumerInterface;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.DeleteNsdRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.DeleteNsdResponse;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.DeletePnfdRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.DeletePnfdResponse;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.DeleteVnfPackageRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.DisableNsdRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.DisableVnfPackageRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.EnableNsdRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.EnableVnfPackageRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.FetchOnboardedVnfPackageArtifactsRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.OnBoardVnfPackageRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.OnBoardVnfPackageResponse;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.OnboardAppPackageRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.OnboardAppPackageResponse;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.OnboardNsdRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.OnboardPnfdRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.QueryNsdResponse;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.QueryOnBoadedAppPkgInfoResponse;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.QueryOnBoardedVnfPkgInfoResponse;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.QueryPnfdResponse;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.UpdateNsdRequest;
import it.nextworks.nfvmano.libs.catalogues.interfaces.messages.UpdatePnfdRequest;
import it.nextworks.nfvmano.libs.common.enums.OperationStatus;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.MethodNotImplementedException;
import it.nextworks.nfvmano.libs.common.exceptions.NotExistingEntityException;
import it.nextworks.nfvmano.libs.common.messages.GeneralizedQueryRequest;
import it.nextworks.nfvmano.libs.common.messages.SubscribeRequest;
import it.nextworks.nfvmano.libs.osmanfvo.nslcm.interfaces.NsLcmConsumerInterface;
import it.nextworks.nfvmano.libs.osmanfvo.nslcm.interfaces.messages.CreateNsIdentifierRequest;
import it.nextworks.nfvmano.libs.osmanfvo.nslcm.interfaces.messages.HealNsRequest;
import it.nextworks.nfvmano.libs.osmanfvo.nslcm.interfaces.messages.InstantiateNsRequest;
import it.nextworks.nfvmano.libs.osmanfvo.nslcm.interfaces.messages.QueryNsResponse;
import it.nextworks.nfvmano.libs.osmanfvo.nslcm.interfaces.messages.ScaleNsRequest;
import it.nextworks.nfvmano.libs.osmanfvo.nslcm.interfaces.messages.TerminateNsRequest;
import it.nextworks.nfvmano.libs.osmanfvo.nslcm.interfaces.messages.UpdateNsRequest;
import it.nextworks.nfvmano.libs.osmanfvo.nslcm.interfaces.messages.UpdateNsResponse;
import it.nextworks.nfvmano.nfvodriver.NfvoAbstractDriver;
import it.nextworks.nfvmano.nfvodriver.NfvoDriverType;
import it.nextworks.nfvmano.nfvodriver.nfvopoller.NfvoOperationPollingManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.Properties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * Created by Juan Brenes on 20/04/18.
 *
 * @author Juan Brenes <juan.brenesbaranzano AT atos.net>
 */

 public class SMDriver extends NfvoAbstractDriver {

    private static final Logger log = LoggerFactory.getLogger(SMDriver.class);
    private SMRestClient restClient;
    @Autowired
	private NfvoOperationPollingManager nfvoOperationPollingManager;

   
    private String createNsIdentifierTemplate;
    private String baseUrl;
    
    private String instantiateNsTemplate =  "/ns/{nsid}/instantiate";
    private String  nSStatusTemplate;
    private String getOperationStatusTemplate = "/operation/{operationid}";
    private Properties props = new Properties();
    private String terminateNsTemplate = "/ns/{nsid}/terminate";
    private String queryNsdTemplate = "/ns/nsd/{nsdid}/{nsdversion}";
    private String queryOnBoardedVnfPkgInfoResponseTemplate = "/ns/vnfd/{vnfdid}/{vnfdversion}";
    private String queryNsiStatusTemplate = "/ns/{nsiid}";
    
    
    public SMDriver(){

        super(NfvoDriverType.SM, "localhost", null);
        this.loadProperties();
        String baseAddress = this.baseUrl.replaceAll("\\{nfvoaddress\\}", "localhost");
        this.restClient = new SMRestClient(baseAddress, new RestTemplate());
       
    }
    public SMDriver(String nfvoAddress, NfvoOperationPollingManager nfvoOperationPollingManager ) {
        super(NfvoDriverType.SM, nfvoAddress, null);
        log.info("createNsIdentifierTemplate: "+ createNsIdentifierTemplate);
        this.loadProperties();
        String baseAddress = this.baseUrl.replaceAll("\\{nfvoaddress\\}", nfvoAddress);   
        this.restClient = new SMRestClient(baseAddress, new RestTemplate());
        this.nfvoOperationPollingManager = nfvoOperationPollingManager;
    }

    private void loadProperties(){
        InputStream fins = getClass().getClassLoader().getResourceAsStream("sm_driver.properties");
        try{
            if(fins!=null){
                    props.load(fins);
            }else{
                log.warn("Null props file");
            }
        }catch (IOException e) {
            log.warn(e.getMessage());
        }
        this.createNsIdentifierTemplate = props.getProperty("create_ns_identifier", "/ns");
        log.info("createNsIdentifierTemplate: "+ createNsIdentifierTemplate);
        this.instantiateNsTemplate = props.getProperty("instantiate_ns", "/ns/{nsid}/instantiate");
        log.info("instantiateNsTemplate: "+ this.instantiateNsTemplate);
        this.getOperationStatusTemplate = props.getProperty("get_operation_status", "/operation/{operationid}");
        log.info("getOperationStatusTemplate: "+ this.getOperationStatusTemplate);
        this.terminateNsTemplate = props.getProperty("terminate_ns", "/ns/{nsid}/terminate");
        log.info("terminateNsTemplate: "+ this.terminateNsTemplate);
        this.baseUrl = props.getProperty("base_url", "http://{nfvoaddress}:8080/5gt/so/v1");
        log.info("baseUrl: "+ this.baseUrl);
        this.nSStatusTemplate=props.getProperty("ns_status", "/ns/{nsid}");
        log.info("nsStatusTemplate: "+ this.nSStatusTemplate);

    }

    public File fetchOnboardedApplicationPackage(String s) throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        log.info("Received call to fetchOnboardedApplicationPackage, parameters {}.", s);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public QueryOnBoadedAppPkgInfoResponse queryApplicationPackage(GeneralizedQueryRequest generalizedQueryRequest) throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        log.info("Received call to queryApplicationPackage, parameters {}.", generalizedQueryRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public String subscribeMecAppPackageInfo(SubscribeRequest subscribeRequest, MecAppPackageManagementConsumerInterface mecAppPackageManagementConsumerInterface) throws MethodNotImplementedException, MalformattedElementException, FailedOperationException {
        log.info("Received call to subscribeMecAppPackageInfo, parameters {}; {}.", subscribeRequest, mecAppPackageManagementConsumerInterface);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void unsubscribeMecAppPackageInfo(String s) throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        log.info("Received call to unsubscribeMecAppPackageInfo, parameter {}.", s);
    }

    public OnboardAppPackageResponse onboardAppPackage(OnboardAppPackageRequest onboardAppPackageRequest) throws MethodNotImplementedException, AlreadyExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to onboardAppPackage, parameter {}.", onboardAppPackageRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void enableAppPackage(String s) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to enableAppPackage, parameter {}.", s);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void disableAppPackage(String s) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to disableAppPackage, parameter {}.", s);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void deleteAppPackage(String s) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to deleteAppPackage, parameter {}.", s);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void abortAppPackageDeletion(String s) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to abortAppPackageDeletion, parameter {}.", s);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public String onboardNsd(OnboardNsdRequest onboardNsdRequest) throws MethodNotImplementedException, MalformattedElementException, AlreadyExistingEntityException, FailedOperationException {
        log.info("Received call to onboardNsd, parameter {}.", onboardNsdRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void enableNsd(EnableNsdRequest enableNsdRequest) throws MethodNotImplementedException, MalformattedElementException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to enableNsd, parameter {}.", enableNsdRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void disableNsd(DisableNsdRequest disableNsdRequest) throws MethodNotImplementedException, MalformattedElementException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to disableNsd, parameter {}.", disableNsdRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public String updateNsd(UpdateNsdRequest updateNsdRequest) throws MethodNotImplementedException, MalformattedElementException, AlreadyExistingEntityException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to updateNsd, parameter {}.", updateNsdRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public DeleteNsdResponse deleteNsd(DeleteNsdRequest deleteNsdRequest) throws MethodNotImplementedException, MalformattedElementException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to deleteNsd, parameter {}.", deleteNsdRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public QueryNsdResponse queryNsd(GeneralizedQueryRequest generalizedQueryRequest) throws MethodNotImplementedException, MalformattedElementException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to queryNsd, parameter {}.", generalizedQueryRequest);
        return this.restClient.queryNsd(this.queryNsdTemplate, generalizedQueryRequest);
    }

    public String subscribeNsdInfo(SubscribeRequest subscribeRequest, NsdManagementConsumerInterface nsdManagementConsumerInterface) throws MethodNotImplementedException, MalformattedElementException, FailedOperationException {
        log.info("Received call to subscribeNsdInfo, parameters {}; {}.", subscribeRequest, nsdManagementConsumerInterface);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void unsubscribeNsdInfo(String s) throws MethodNotImplementedException, MalformattedElementException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to unsubscribeNsdInfo, parameter {}.", s);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public String onboardPnfd(OnboardPnfdRequest onboardPnfdRequest) throws MethodNotImplementedException, MalformattedElementException, AlreadyExistingEntityException, FailedOperationException {
        log.info("Received call to onboardPnfd, parameter {}.", onboardPnfdRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public String updatePnfd(UpdatePnfdRequest updatePnfdRequest) throws MethodNotImplementedException, MalformattedElementException, NotExistingEntityException, AlreadyExistingEntityException, FailedOperationException {
        log.info("Received call to updatePnfd, parameter {}.", updatePnfdRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public DeletePnfdResponse deletePnfd(DeletePnfdRequest deletePnfdRequest) throws MethodNotImplementedException, MalformattedElementException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to deletePnfd, parameter {}.", deletePnfdRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public QueryPnfdResponse queryPnfd(GeneralizedQueryRequest generalizedQueryRequest) throws MethodNotImplementedException, MalformattedElementException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to queryPnfd, parameter {}.", generalizedQueryRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public OnBoardVnfPackageResponse onBoardVnfPackage(OnBoardVnfPackageRequest onBoardVnfPackageRequest) throws MethodNotImplementedException, AlreadyExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to onBoardVnfPackage, parameter {}.", onBoardVnfPackageRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void enableVnfPackage(EnableVnfPackageRequest enableVnfPackageRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to enableVnfPackage, parameter {}.", enableVnfPackageRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void disableVnfPackage(DisableVnfPackageRequest disableVnfPackageRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to disableVnfPackage, parameter {}.", disableVnfPackageRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void deleteVnfPackage(DeleteVnfPackageRequest deleteVnfPackageRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to deleteVnfPackage, parameter {}.", deleteVnfPackageRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public QueryOnBoardedVnfPkgInfoResponse queryVnfPackageInfo(GeneralizedQueryRequest generalizedQueryRequest) throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        log.info("Received call to queryVnfPackageInfo, parameter {}.", generalizedQueryRequest);
        return restClient.queryVnfPackageInfo(this.queryOnBoardedVnfPkgInfoResponseTemplate, generalizedQueryRequest);    
        }

    public String subscribeVnfPackageInfo(SubscribeRequest subscribeRequest, VnfPackageManagementConsumerInterface vnfPackageManagementConsumerInterface) throws MethodNotImplementedException, MalformattedElementException, FailedOperationException {
        log.info("Received call to subscribeVnfPackageInfo, parameters {}; {}.", subscribeRequest, vnfPackageManagementConsumerInterface);
        throw new MethodNotImplementedException("not supported in SM.");    }

    public void unsubscribeVnfPackageInfo(String s) throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        log.info("Received call to unsubscribeVnfPackageInfo, parameter {}.", s);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public File fetchOnboardedVnfPackage(String s) throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        log.info("Received call to fetchOnboardedVnfPackage, parameter {}.", s);
        throw new MethodNotImplementedException("fetchOnboardedVnfPackageArtifacts not supported in SM.");
    }

    public List<File> fetchOnboardedVnfPackageArtifacts(FetchOnboardedVnfPackageArtifactsRequest fetchOnboardedVnfPackageArtifactsRequest) throws MethodNotImplementedException, NotExistingEntityException, MalformattedElementException {
        log.info("Received call to fetchOnboardedVnfPackageArtifacts, parameter {}.", fetchOnboardedVnfPackageArtifactsRequest);
        throw new MethodNotImplementedException("fetchOnboardedVnfPackageArtifacts not supported in SM.");
    }

    public void abortVnfPackageDeletion(String s) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to abortVnfPackageDeletion, parameter {}.", s);
        throw new MethodNotImplementedException("abortVnfPackageDeletion not supported in SM.");

    }

    public void queryVnfPackageSubscription(GeneralizedQueryRequest generalizedQueryRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to queryVnfPackageSubscription, parameter {}.", generalizedQueryRequest);
        throw new MethodNotImplementedException("queryVnfPackageSubscription not supported in SM.");
    }

    public String createNsIdentifier(CreateNsIdentifierRequest createNsIdentifierRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to createNsIdentifier, parameter {}.", createNsIdentifierRequest);
        return this.restClient.createNsIdentifier(this.createNsIdentifierTemplate, createNsIdentifierRequest).getNSId();
    }

    public String instantiateNs(InstantiateNsRequest instantiateNsRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to instantiateNs, parameter {}.", instantiateNsRequest);
        	
        String operationId =  this.restClient.instantiateNs(this.instantiateNsTemplate, instantiateNsRequest).getOperationId();
        nfvoOperationPollingManager.addOperation(operationId, OperationStatus.SUCCESSFULLY_DONE, instantiateNsRequest.getNsInstanceId(), "NS_INSTANTIATION");
		log.debug("Added polling task for NFVO operation " + operationId);
        return operationId;
    }

    public String scaleNs(ScaleNsRequest scaleNsRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to scaleNs, parameter {}.", scaleNsRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public UpdateNsResponse updateNs(UpdateNsRequest updateNsRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to updateNs, parameter {}.", updateNsRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public QueryNsResponse queryNs(GeneralizedQueryRequest generalizedQueryRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to queryNs, parameter {}.", generalizedQueryRequest);
        return this.restClient.queryNs(this.queryNsiStatusTemplate, generalizedQueryRequest);
    }

    public String terminateNs(TerminateNsRequest terminateNsRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to terminateNs, parameter {}.", terminateNsRequest);
        String operationId = this.restClient.terminateNs(terminateNsTemplate, terminateNsRequest).getOperationId();
		nfvoOperationPollingManager.addOperation(operationId, OperationStatus.SUCCESSFULLY_DONE, terminateNsRequest.getNsInstanceId(), "NS_TERMINATION");
		log.debug("Added polling task for NFVO operation " + operationId);
        return operationId;
    }

    public void deleteNsIdentifier(String s) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to deleteNsIdentifier, parameter {}.", s);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public String healNs(HealNsRequest healNsRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to healNs, parameter {}.", healNsRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public OperationStatus getOperationStatus(String s) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to getOperationStatus, parameter {}.", s);
        return this.restClient.getOperationStatus(this.getOperationStatusTemplate,s);
    }

    public String subscribeNsLcmEvents(SubscribeRequest subscribeRequest, NsLcmConsumerInterface nsLcmConsumerInterface) throws MethodNotImplementedException, MalformattedElementException, FailedOperationException {
        log.info("Received call to subscribeNsLcmEvents, parameters {}; {}.", subscribeRequest, nsLcmConsumerInterface);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void unsubscribeNsLcmEvents(String s) throws MethodNotImplementedException, MalformattedElementException, NotExistingEntityException, FailedOperationException {
        log.info("Received call to unsubscribeNsLcmEvents, parameter {}.", s);
        throw new MethodNotImplementedException("not supported in SM.");
    }

    public void queryNsSubscription(GeneralizedQueryRequest generalizedQueryRequest) throws MethodNotImplementedException, NotExistingEntityException, FailedOperationException, MalformattedElementException {
        log.info("Received call to queryNsSubscription, parameter {}.", generalizedQueryRequest);
        throw new MethodNotImplementedException("not supported in SM.");
    }
}
