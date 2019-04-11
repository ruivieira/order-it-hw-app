package org.jbpm.cases.orderithwapp;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jbpm.document.service.impl.DocumentImpl;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.cases.CaseFile;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.CaseServicesClient;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.UserTaskServicesClient;

public class OrderItHwRemoteScripts {

	private static final String URL = "http://localhost:8090/rest/server";
    private static final MarshallingFormat FORMAT = MarshallingFormat.JSON;
	
	public OrderItHwRemoteScripts() {
		
	}
	
	public static void main(String[] args) {
		OrderItHwRemoteScripts remoteScripts = new OrderItHwRemoteScripts();
//		for (int i = 0; i < 5; i++) {
//			remoteScripts.startAndApproveAndCloseCase("maciek", "maciek1!", "apple", true);
//			remoteScripts.startAndApproveAndCloseCase("maciek", "maciek1!", "lenovo", true);
//			remoteScripts.startAndApproveAndCloseCase("mary", "mary1!", "apple", false);
//			remoteScripts.startAndApproveAndCloseCase("mary", "mary1!", "lenovo", false);
//		}
		String caseId = remoteScripts.startCase("maciek", "maciek1!", "apple", "krisv");
		remoteScripts.claimAndCompleteSupplierCaseTask("itorders", caseId, "tihomir", "tihomir1!");
		
		remoteScripts.addDynamicTask("itorders", caseId, "krisv", "krisv1!", "Contact legal", "krisv", "Recommendation: For user 'maciek': Please notify legal of your hardware request if necessary.");
//		remoteScripts.addDynamicTask("itorders", caseId, "krisv", "krisv1!", "Contact legal", "maciek", "Please notify legal of your hardware request if necessary.");
	}
	
	public void startAndApproveAndCloseCase(String userId, String password, String vendor, boolean approve) {
		String caseId = startCase(userId, password, vendor, "krisv");
		System.out.println(userId + " started case " + caseId);
		claimAndCompleteSupplierCaseTask("itorders", caseId, "tihomir", "tihomir1!");
		completeManagerCaseTask("itorders", caseId, "krisv", "krisv1!", approve);
		closeCase(userId,  password, caseId);
	}
    
	public String startCase(String owner, String password, String vendor, String manager) {
		KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(URL, owner, password);
        conf.setMarshallingFormat(FORMAT);
        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);
        CaseServicesClient caseServicesClient = kieServicesClient.getServicesClient(CaseServicesClient.class);
        Map<String, Object> data = new HashMap<>();
        data.put("vendor", vendor);
        CaseFile caseFile = CaseFile.builder()
            .addUserAssignments("owner", owner)
            .addUserAssignments("manager", manager)
            .addGroupAssignments("supplier", vendor)
            .data(data)
            .build();
        return caseServicesClient.startCase("itorders", "itorders.orderhardware", caseFile);
	}
	
	public void claimAndCompleteSupplierCaseTask(String containerId, String caseId, String userId, String password) {
		KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(URL, userId, password);
        conf.setMarshallingFormat(FORMAT);
        Set<Class<?>> extraJaxbClassList = new HashSet<Class<?>>();
        extraJaxbClassList.add(DocumentImpl.class);
        conf.addExtraClasses(extraJaxbClassList);
        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);
        CaseServicesClient caseServicesClient = kieServicesClient.getServicesClient(CaseServicesClient.class);
        UserTaskServicesClient userTaskServicesClient = kieServicesClient.getServicesClient(UserTaskServicesClient.class);
        List<TaskSummary> tasks = caseServicesClient.findCaseTasksAssignedAsPotentialOwner(caseId, userId, 0, 10);
		Long taskId = tasks.get(0).getId();
		System.out.println(userId + " completing task " + taskId + " " + tasks.get(0).getName());
		userTaskServicesClient.claimTask(containerId, taskId, userId);
		userTaskServicesClient.startTask(containerId, taskId, userId);
		Map<String, Object> data = new HashMap<String, Object>();
		byte[] docContent = "first case document".getBytes();
        DocumentImpl document = new DocumentImpl(UUID.randomUUID().toString(), "test case doc", docContent.length, new Date());
    	document.setContent(docContent);
		data.put("hwSpec_", document);
		userTaskServicesClient.completeTask(containerId, taskId, userId, data);
	}

	public void completeManagerCaseTask(String containerId, String caseId, String userId, String password, boolean approved) {
		KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(URL, userId, password);
        conf.setMarshallingFormat(FORMAT);
        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);
        CaseServicesClient caseServicesClient = kieServicesClient.getServicesClient(CaseServicesClient.class);
        UserTaskServicesClient userTaskServicesClient = kieServicesClient.getServicesClient(UserTaskServicesClient.class);
        List<TaskSummary> tasks = caseServicesClient.findCaseTasksAssignedAsPotentialOwner(caseId, userId, 0, 10);
		Long taskId = tasks.get(0).getId();
		System.out.println(userId + " completing task " + taskId + " " + tasks.get(0).getName());
		userTaskServicesClient.startTask(containerId, taskId, userId);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("approved_", approved);
		params.put("approved", approved);
		userTaskServicesClient.completeTask(containerId, taskId, userId, params);
	}
	
	public void closeCase(String owner, String password, String caseId) {
		KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(URL, owner, password);
        conf.setMarshallingFormat(FORMAT);
        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);
        CaseServicesClient caseServicesClient = kieServicesClient.getServicesClient(CaseServicesClient.class);
        caseServicesClient.closeCaseInstance("itorders", caseId, "Automatically closing case");
	}
	
	public void addDynamicTask(String containerId, String caseId, String userId, String password, String taskName, String actorId, String description) {
		KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(URL, userId, password);
        conf.setMarshallingFormat(FORMAT);
        KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(conf);
        CaseServicesClient caseServicesClient = kieServicesClient.getServicesClient(CaseServicesClient.class);
        Map<String, Object> data = new HashMap<String, Object>();
        caseServicesClient.addDynamicUserTask(containerId, caseId, taskName, description, actorId, null, data);
	}

}
