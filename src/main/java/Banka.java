import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

public class Banka {

	static List<Task> tasksUser;
	static List<Task> tasksEmployee;
	static String employeeRole;
	static int amountOfLoan;
	static ProcessEngine pe;
	static ProcessInstance pi;
	static Map<String, Object> data;

	public static void main(String[] args) throws FileNotFoundException {
		pe = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().buildProcessEngine();

		try {
			// DEPLOY PROCESU
			RepositoryService repositoryService = pe.getRepositoryService();
			repositoryService.createDeployment().addClasspathResource("diagrams/Banka.bpmn").deploy();

			// START PROCESU
			pi = startProcessInstance(pe);		
			tasksUser = getTasks(pe, "ziadatel");	
			data = setData();
			applyForLoan();
			
			
		} finally {			
			if (pe != null) {
				pe.close();
			}
		}
	}
	
	private static ProcessInstance startProcessInstance(ProcessEngine pe) {
		RuntimeService runtimeService = pe.getRuntimeService();
		pi = runtimeService.startProcessInstanceByKey("banka");
		return pi;
	}

	private static List<Task> getTasks(ProcessEngine pe, String assignee) {
		List<Task> list = pe.getTaskService().createTaskQuery().taskAssignee(assignee).list();
		return list;
	
	}
	
private static void applyForLoan() {
	
		boolean vyplneneUdaje = fundamentalCheckOfLoan(data);
		for (Task task : tasksUser) {
			if (task.getAssignee().equals("ziadatel")) {				
				pe.getRuntimeService().setVariables(pi.getId(), data);
		while(!vyplneneUdaje) {			
			data=setData();
			vyplneneUdaje = fundamentalCheckOfLoan(data);
		}
				pe.getTaskService().complete(task.getId(), data);		
			
		
		
			}
		}
	}


	public static boolean fundamentalCheckOfLoan(Map<String, Object> data) {	
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			if (entry.getValue() == null) {
				data.put("kontrolaUdajov", "FALSE");
				System.err.println("Niektore udaje nie su vyplnene!");			
				return false;
			}
		}
		System.out.println("Vsetky udaje su vyplnene.");
		data.put("kontrolaUdajov", "TRUE");
		return true;
	}

	public static void checkForPreApproved(DelegateExecution execution) {
		Map<String, Object> data = execution.getVariables();
		int lastMonths = Integer.parseInt((String) data.get("posledneMesiace").toString());
		int yearly = Integer.parseInt((String) data.get("rocnyPrijem").toString());
		amountOfLoan = Integer.parseInt((String) data.get("pozadovanaVyska").toString());
		if (yearly * 5 > amountOfLoan && lastMonths * 4 >= yearly) {
			execution.setVariable("predschvalene", "FALSE");
			System.err.println("Ziadost o uver nebola schvalena!");
		} else {
			execution.setVariable("predschvalene", "TRUE");
			System.out.println("Ziadost o uver bola predschvalena.");
		}
		
	}

	private static void addEmployeeRole(DelegateExecution execution) {
		if (amountOfLoan >= 100000) {
			employeeRole = "veduci";
			execution.setVariable("vybavuje", "veduci");
		} else {
			employeeRole = "zamestnanec";
			execution.setVariable("vybavuje", "zamestnanec");
		}
	}

	public static void decideAboutLoan(DelegateExecution execution) {
		if (Math.random() > 0.5) {
			if (employeeRole.equals("veduci")) {
				pe.getRuntimeService().setVariable(pi.getId(), "schvaleneVeducim", "TRUE");
			} else {
				pe.getRuntimeService().setVariable(pi.getId(), "schvaleneZamestnancom", "TRUE");
			}		
		
		} else {
			if (employeeRole.equals("veduci")) {
				pe.getRuntimeService().setVariable(pi.getId(), "schvaleneVeducim", "FALSE");				
				System.out.println("Ziadost o uver nebola schvalena!");
			} else {
				pe.getRuntimeService().setVariable(pi.getId(), "schvaleneZamestnancom", "FALSE");
			}
		}
		if(pe.getRuntimeService().getVariable(pi.getId(), "schvaleneVeducim").equals("TRUE")) {
			System.out.println("Ziadost o uver bola schvalena.");
		}
		
		if(pe.getRuntimeService().getVariable(pi.getId(), "schvaleneVeducim").equals("FALSE")) {
			System.out.println("Ziadost o uver nebola schvalena.");
		}
		
		
	}

	private static Map<String, Object> setData() {
		Map<String, Object> newMap = new HashMap<String, Object>();
		if (Math.random() > 0.005) {
			newMap.put("rodneCislo", RC[(int) (Math.random() * RC.length)]);
		} else {
			newMap.put("rodneCislo", null);
		}
		if (Math.random() > 0.005) {
			newMap.put("meno", KM[(int) (Math.random() * KM.length)]);
		} else {
			newMap.put("meno", null);
		}

		if (Math.random() > 0.005) {
			newMap.put("priezvisko", P[(int) (Math.random() * P.length)]);
		} else {
			newMap.put("priezvisko", null);
		}

		if (Math.random() > 0.005) {
			newMap.put("pozadovanaVyska", (int) (Math.random() * 200000));
		} else {
			newMap.put("pozadovanaVyska", null);
		}
		if (Math.random() > 0.005) {
			int yearly = (int) (Math.random() * 100000) + 8000;
			newMap.put("rocnyPrijem", yearly);
			// in case applicant have some financial problems(only 1/3
			// probability)
			if (Math.random() > 0.33) {
				newMap.put("posledneMesiace", yearly / 4 + 1);
			} else {
				newMap.put("posledneMesiace", yearly / 7);
			}
		} else {
			newMap.put("rocnyPrijem", null);
			newMap.put("posledneMesiace", null);
		}
		newMap.put("schvaleneZamestnancom", "FALSE");
		newMap.put("schvaleneVeducim", "FALSE");
		return newMap;
	}

	private static final String[] RC = { "9158046324", "915809468", "9158046684", "5158046324", "9158046354" };
	private static final String[] P = { "Novakova", "Kovacova", "Janosikova", "Kopacikova", "Klepacova" };
	private static final String[] KM = { "Katka", "Janka", "Mirka", "Marta", "Viktoria" };

}
