package spb;


import jade.core.Runtime;

import java.util.Random;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class MainContainer {

	Runtime rt;
	ContainerController container;

	public ContainerController initContainerInPlatform(String host, String port, String containerName) {
		// Get the JADE runtime interface (singleton)
		this.rt = Runtime.instance();

		// Create a Profile, where the launch arguments are stored
		Profile profile = new ProfileImpl();
		profile.setParameter(Profile.CONTAINER_NAME, containerName);
		profile.setParameter(Profile.MAIN_HOST, host);
		profile.setParameter(Profile.MAIN_PORT, port);
		// create a non-main agent container
		ContainerController container = rt.createAgentContainer(profile);
		return container;
	}

	public void initMainContainerInPlatform(String host, String port, String containerName) {

		// Get the JADE runtime interface (singleton)
		this.rt = Runtime.instance();

		// Create a Profile, where the launch arguments are stored
		Profile prof = new ProfileImpl();
		prof.setParameter(Profile.CONTAINER_NAME, containerName);
		prof.setParameter(Profile.MAIN_HOST, host);
		prof.setParameter(Profile.MAIN_PORT, port);
		prof.setParameter(Profile.MAIN, "true");
		prof.setParameter(Profile.GUI, "true");

		// create a main agent container
		this.container = rt.createMainContainer(prof);
		rt.setCloseVM(true);

	}

	public void startAgentInPlatform(String name, String classpath) {
		try {
			AgentController ac = container.createNewAgent(name, classpath, new Object[0]);
			ac.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		MainContainer a = new MainContainer();
		
		a.initMainContainerInPlatform("localhost", "9888", "MainContainer");
		
		a.startAgentInPlatform("AE1", "spb.AgenteEstacao");
		a.startAgentInPlatform("AE2", "spb.AgenteEstacao");
		a.startAgentInPlatform("AE3", "spb.AgenteEstacao");
		a.startAgentInPlatform("AE4", "spb.AgenteEstacao");
		a.startAgentInPlatform("AE5", "spb.AgenteEstacao");
		a.startAgentInPlatform("AE6", "spb.AgenteEstacao");
		a.startAgentInPlatform("AE7", "spb.AgenteEstacao");
		a.startAgentInPlatform("AE8", "spb.AgenteEstacao");
		a.startAgentInPlatform("AE9", "spb.AgenteEstacao");
		a.startAgentInPlatform("AE10", "spb.AgenteEstacao");
		
		Random rand = new Random();
		int numUtilizadores;
		String nomeUtilizador;
		
		Thread.sleep(1000);
		a.startAgentInPlatform("AI", "spb.AgenteInterface");
		int i = 1;
		while(true)
		{
			if(i == 1)
				numUtilizadores = rand.nextInt(15-10)+10; //cria um número aleatório entre 10 e 15
			else
				numUtilizadores = rand.nextInt(7-5)+5; //cria um número aleatório entre 5 e 7
			for(int j = 0; j < numUtilizadores; i++, j++)
			{
				nomeUtilizador = "AU"+i;
				a.startAgentInPlatform(nomeUtilizador, "spb.AgenteUtilizador");
			}
			Thread.sleep(15000);
		}

	}
}