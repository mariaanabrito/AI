package spb;

import java.util.ArrayList;
import static java.lang.Math.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jess.*;

public class AgenteEstacao extends Agent
{
	private Rete engine;
	//lista de utilizadores que se encontram na área da estação
	private List<String> utilsArea;
	//lista de utilizadores que pretendem acabar o percurso nesta estação
	private Map<String, Utilizador> utilsFinal;
	private Map<String, Float> incentivos;
	private int capMax;
	private int capAtual; // n bicicletas armazenadas
	private int numDevolucoes, numAlugueres, numEntradas, numPedidos;
	
	private final float OCUPACAO_CRITICA = 0.9f;
	
	
	public class EnviaIncentivo extends TickerBehaviour
	{
		public EnviaIncentivo(Agent a, long timeout)
		{
			super(a, timeout);
		}
		public void onTick()
		{
			if(utilsArea.size() > 0)
			{
				float incentivo = (float)capAtual / (float)capMax;
				incentivo = 1.0f - incentivo;
				ACLMessage acl = new ACLMessage(ACLMessage.INFORM);
				acl.setContent(incentivo + "");
				for(String nome : utilsArea)
					if(!utilsFinal.containsKey(nome))
						acl.addReceiver(new AID(nome, AID.ISLOCALNAME));
				
				if(incentivo > 0.0f)//ou seja, se a estação estiver cheia não envia incentivos
					send(acl);
			}
		}
	}
	
	public class PedeIncentivoEstacao extends SimpleBehaviour
	{
		private String nome;
		private final long TIMEOUT = 2500;
		private long wakeUpTime; 
		private boolean done;
		private ACLMessage msg;
		
		public PedeIncentivoEstacao(String nome)
		{
			this.nome = nome;
			done = false;			
		}
		
		public void onStart()
		{
			wakeUpTime = System.currentTimeMillis() + TIMEOUT;
		}
		
		@Override
		public void action() {
			
			if(System.currentTimeMillis() < wakeUpTime)
			{
				
				msg = new ACLMessage(ACLMessage.REQUEST);
				msg.addReceiver(new AID(nome, AID.ISLOCALNAME));
				msg.setContent("incentivo");
				send(msg);
				
				MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				MessageTemplate mt2 = MessageTemplate.MatchOntology("pedido de incentivo");
				MessageTemplate mt3 = MessageTemplate.MatchSender(new AID(nome, AID.ISLOCALNAME));
				MessageTemplate mt4 = MessageTemplate.and(mt1, mt2);
				MessageTemplate mt = MessageTemplate.and(mt3, mt4);
				
				ACLMessage acl = receive(mt);
				if(acl != null)
				{
					float inc = Float.parseFloat(acl.getContent());
					incentivos.put(nome, inc);
					done = true;
					
				}
			}
			else
			{
				incentivos.put(nome, 0.0f);
				done = true;
			}
		}

		@Override
		public boolean done() 
		{
			
			return done;
		}

	}

	public class Emergencia extends SimpleBehaviour
	{
		private float xi, yi, xf, yf;
		private String nome;
		public Emergencia(float x, float y, String n)
		{
			xi = x;
			yi = y;
			nome = n;
		}
		
		public boolean intersetaArea(double cx, double cy, float r, String n)
		{

				double distF = sqrt(pow(xi - xf, 2)  + pow(yi - yf, 2));
				
			 	double baX = xf - xi;
		        double baY = yf - yi;
		        double caX = cx - xi;
		        double caY = cy - yi;

		        double a = baX * baX + baY * baY;
		        double bBy2 = baX * caX + baY * caY;
		        double c = caX * caX + caY * caY - r * r;

		        double pBy2 = bBy2 / a;
		        double q = c / a;

		        double disc = pBy2 * pBy2 - q;
		        if (disc < 0) {
		            return false;
		        }
		        else 
		        {
		        	double p1x, p1y, p2x, p2y;
		        	
		        	double tmpSqrt = sqrt(disc);
		            double abScalingFactor1 = -pBy2 + tmpSqrt;
		            double abScalingFactor2 = -pBy2 - tmpSqrt;

		            p1x = xi - baX * abScalingFactor1;
		            p1y = yi - baY * abScalingFactor1;
		            
		            if (disc == 0)  
		            	return true;
		            else
		            {
		            	p2x = xi - baX * abScalingFactor2;
		            	p2y = yi- baY * abScalingFactor2;
		            	

		            	
		            	double dist1 = sqrt(pow(xi - p1x, 2)  + pow(yi - p1y, 2) );
		            	double dist2 = sqrt(pow(xf - p1x, 2)  + pow(yf - p1y, 2) );
		            	
		            	double dist3 = sqrt(pow(xi - p2x, 2)  + pow(yi - p2y, 2) );
		            	double dist4 = sqrt(pow(xf - p2x, 2)  + pow(yf - p2y, 2) );

		            	if((dist1 + dist2) == distF || (dist3 + dist4) == distF)
		            			return true;

		            	return false;
		            }
   		
		        }
		}
		
		public void action()
		{
			Estacao e;
			Map<String, Estacao> todasEstacoes = new HashMap<>();
			Map<String, Estacao> estacoesIntermedias = new HashMap<>();
			incentivos = new HashMap<>();
			
			QueryResult rs;
			try 
			{
				rs = engine.runQueryStar("procuraEstacoes", new ValueVector());
				
				while(rs.next())
				{
					//(estacoes (nome ?n)(raio ?r)(capacidadeMax ?max)(coords ?x ?y))
					e = new Estacao(rs.getString("n"), rs.getDouble("x"), rs.getDouble("y"), rs.getFloat("r"));
					
					if(e.getEstacao().equals(myAgent.getLocalName()))
					{
						xf = rs.getFloat("x");
						yf = rs.getFloat("y");
					}
					todasEstacoes.put(e.getEstacao(), e);
				}
				
				for(Map.Entry<String, Estacao> entry : todasEstacoes.entrySet())
				{
					if(!entry.getKey().equals(myAgent.getLocalName()))
					{
						if(intersetaArea(entry.getValue().getCoordX(), entry.getValue().getCoordY(), entry.getValue().getRaio(), entry.getKey()))
						{
							estacoesIntermedias.put(entry.getKey(), entry.getValue());
						}
					}
				}
				
				if(estacoesIntermedias.isEmpty())
				{
					//para um raio de x metros da estação final, mete-as no map
					//o x pode ser a distância dos 3/4
					double raio = sqrt(pow(xi - xf, 2)  + pow(yi - yf, 2));
					for(Map.Entry<String, Estacao> entry : todasEstacoes.entrySet())
					{
						double dist = sqrt(pow(xf - entry.getValue().getCoordX(), 2)  + pow(yf - entry.getValue().getCoordY(), 2));
						if(dist < raio)
						{
							estacoesIntermedias.put(entry.getKey(), entry.getValue());
						}
					}
				}
				
				estacoesIntermedias.remove(myAgent.getLocalName());

				ParallelBehaviour estacaoCheia =  new ParallelBehaviour(myAgent, ParallelBehaviour.WHEN_ALL){
							public int onEnd()
							{
								float max = 0.0f;
								String nomeMAX="";
								for(Map.Entry<String, Float> entry : incentivos.entrySet())
								{
									if(entry.getValue() > max)
									{
										max = entry.getValue();
										nomeMAX = entry.getKey();
									}
								}
		
								if(max == 0.0f)
									System.out.println("A estação " + myAgent.getLocalName() + " não obteve ajuda de nenhuma estação.");
								else
								{
									System.out.println("Emergência: o melhor incentivo recebido por "+ myAgent.getLocalName() + " foi " + max + " e pertence à estação " + nomeMAX);
									ACLMessage acl = new ACLMessage(ACLMessage.INFORM);
									acl.setContent("incentivo max");
									acl.addReceiver(new AID(nomeMAX, AID.ISLOCALNAME));
									acl.addReplyTo(new AID(nome, AID.ISLOCALNAME));
									send(acl);
									utilsFinal.remove(nome);
								}
								
								return 1;
							}
						};
						
				for(Map.Entry<String, Estacao> entry : estacoesIntermedias.entrySet())
				{
					estacaoCheia.addSubBehaviour(new PedeIncentivoEstacao(entry.getKey()));
				}
				
				myAgent.addBehaviour(estacaoCheia);
				
			} catch (JessException ex) {
				ex.printStackTrace();
			}

		}
		public boolean done()
		{
			return true;
		}
	}

	public class Controlador extends CyclicBehaviour
	{
		public void action()
		{
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			
			ACLMessage msg = receive(mt); 
			if(msg != null)
			{
				String sender = msg.getSender().getLocalName();
				if(msg.getContent().equals("in"))
				{
					utilsArea.add(sender);
					numEntradas++;	
				}
				else if(msg.getContent().equals("out"))
				{
					utilsArea.remove(sender);
				}
				else if(msg.getContent().equals("incentivo max"))
				{
					ACLMessage acl = msg.createReply();
					acl.setContent("0.75");
					send(acl);
				}
				else
				{
					String content = msg.getContent();
					String [] par = content.split(" ");
					if(par[0].equals("coords"))
					{
						float x, y;
						x = Float.parseFloat(par[1]);
						y = Float.parseFloat(par[2]);
						System.out.println("O utilizador " + sender + " vai a 3/4 do percurso e está em x:" + x + " e y:" + y);
						Utilizador u = new Utilizador(sender, x, y);
						utilsFinal.put(u.getNome(), u);
						
						float ocupacao = (float) capAtual / (float) capMax;
						if(ocupacao >= OCUPACAO_CRITICA )
						{
							myAgent.addBehaviour(new Emergencia(x, y, u.getNome()));
						}
					}
				}
			}
		}
	}
	
	public class RecebePedido extends CyclicBehaviour
	{
		public void action()
		{
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = receive(mt);
			if(msg != null)
			{
				ACLMessage acl;
				String sender = msg.getSender().getLocalName();
				if(msg.getContent().equals("stats"))
				{
					acl = new ACLMessage(ACLMessage.INFORM);
					acl.addReceiver(msg.getSender());
					float ocupacao = (float) capAtual / (float) capMax;
					String stats = numAlugueres + " " + numDevolucoes + " " + numEntradas + " " + capAtual + " " + ocupacao + " " + numPedidos;
					acl.setContent(stats);
					send(acl);
				}
				if(msg.getContent().equals("incentivo"))
				{
						acl = new ACLMessage(ACLMessage.INFORM);
						acl.setOntology("pedido de incentivo");
						acl.addReceiver(msg.getSender());
						float ocup = (float) capAtual / (float) capMax;
						ocup = 1.0f - ocup;
						acl.setContent(ocup + "");
						send(acl);
				}
				if(msg.getContent().equals("bicicleta"))
				{
					
					numPedidos ++;
					if(capAtual > 0)
					{
						capAtual --;
						utilsArea.add(sender);
						numAlugueres++;
						
						acl = new ACLMessage(ACLMessage.AGREE);
						acl.addReceiver(msg.getSender());
						acl.setContent("bicicleta");
						send(acl);
					}
					else
					{
						
						acl = new ACLMessage(ACLMessage.REFUSE);
						acl.addReceiver(msg.getSender());
						acl.setContent("bicicleta");
						send(acl);
					}
				}
				else if(msg.getContent().equals("entrega"))
				{
					if(capAtual == capMax)
					{
						System.out.println("A estação " + myAgent.getLocalName() + " está cheia. Não dá para entregar a bicicleta.");
						acl = new ACLMessage(ACLMessage.REFUSE);
						acl.addReceiver(msg.getSender());
						acl.setContent("entrega");
						send(acl);
						
					}
					else
					{
						capAtual++;
						numDevolucoes++ ;
						utilsArea.remove(sender);
						acl = new ACLMessage(ACLMessage.AGREE);
						acl.addReceiver(msg.getSender());
						acl.setContent("entrega");
						send(acl);
					}
				}
				else if(msg.getContent().equals("incentivo aceite"))
				{
					if(capAtual == capMax)
					{
						acl = new ACLMessage(ACLMessage.DISCONFIRM);
						acl.addReceiver(msg.getSender());
						acl.setContent("lugar reservado");
						send(acl);
					}
					else
					{
						utilsArea.remove(sender);
						capAtual++;
						numDevolucoes++;
						acl = new ACLMessage(ACLMessage.CONFIRM);
						acl.addReceiver(msg.getSender());
						acl.setContent("lugar reservado");
						send(acl);
					}
				}
			}
		}
	}
	
	private void initEstacao()
	{
		engine = new Rete();
		utilsArea = new ArrayList<>();
		utilsFinal = new HashMap<>();
		numDevolucoes = numAlugueres = numEntradas = numPedidos = 0;
		
		try
		{
			engine.batch("spb/Estacoes.clp");
			engine.reset();

			QueryResult rs = engine.runQueryStar("procuraEstacoes", new ValueVector());
			while(rs.next())
			{
				if(rs.getString("n").equals(getAID().getLocalName()))
				{
					capMax = rs.getInt("max");
					capAtual = capMax - 2;
				}
			}
			
			//inserir o agenteestacao no DF
			
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType("estação");
			sd.setName(getAID().toString());
			dfd.addServices(sd);
			
			try {
				DFService.register(this,  dfd);
			} catch (FIPAException e) {
				e.printStackTrace();
			}
		}
		catch(JessException e)
		{
			e.printStackTrace();
		}
	}

	public void setup()
	{
		super.setup();
		initEstacao();
		this.addBehaviour(new Controlador());
		this.addBehaviour(new RecebePedido());
		this.addBehaviour(new EnviaIncentivo(this, 1000));
	}
}
