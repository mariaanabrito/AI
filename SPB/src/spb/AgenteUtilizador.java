package spb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jadex.xml.writer.Writer;
import jess.*;

public class AgenteUtilizador extends Agent{
	
	private Rete engineEstacoes;
	private Rete engineUtilizador;
	private Random rand;
	private List<Estacao> estacoes;
	private Map<String, Estacao> entradas;
	private boolean notifiqueiAgenteFinal;
	private boolean percurso75;
	private boolean fimPercurso;
	private boolean incentivoAceite;
	
	private void initUtilizador()
	{
		
		engineEstacoes = new Rete();
		engineUtilizador = new Rete();
		rand = new Random();
		entradas = new HashMap<>();

		//criar um ficheiro AUX.clp 
		String fileName = getAID().getLocalName() + ".clp";
		File f = new File("src/spb/", fileName);
		try 
		{
			
			f.createNewFile();
			
			//copiar o conteúdo do utilizadorAux.txt para cada agente utilizador
			FileChannel src = new FileInputStream("src/spb/UtilizadorAux.txt").getChannel();
			FileChannel dest = new FileOutputStream("src/spb/" + fileName).getChannel();
			dest.transferFrom(src, 0, src.size());
			src.close();
			dest.close();
			 
			//ler o ficheiro estacoes.clp e obter todas as coordenadas e ids das estações
			engineEstacoes.batch("spb/Estacoes.clp");
			engineEstacoes.reset();

			QueryResult rs = engineEstacoes.runQueryStar("procuraEstacoes", new ValueVector());
			estacoes = new ArrayList<>();
			Estacao e;
			while(rs.next())
			{
				e = new Estacao(rs.getString("n"), rs.getDouble("x"), rs.getDouble("y"), rs.getFloat("r"));
				estacoes.add(e);
			}
			
			//aleatoriamente escolher duas estações (uma para o destino e outra para o início)
		
			int i, j;
			i = rand.nextInt(estacoes.size());
			j = rand.nextInt(estacoes.size());
			Estacao inicio = estacoes.get(i);
			while(i == j)
				j = rand.nextInt(estacoes.size());
			Estacao fim = estacoes.get(j);
			
			//gerar aleatoriamente um número que irá corresponder à velocidade do utilizador entre 3 e 5
			
			int velocidade = rand.nextInt(5-3)+3;
			
			//gerar aleatoriamente um valor de incentivo mínimo que o utilizador poderia aceitar
			float incentivoMin = rand.nextFloat() * (0.7f - 0.4f) + 0.4f;
			
			//assert no facto utilizador do ficheiro AUx.clp do incentivo, destino e partida
			double distTotal = Math.sqrt(Math.pow(fim.getCoordX() - inicio.getCoordX(), 2)  + Math.pow(fim.getCoordY() - inicio.getCoordY(), 2));
			String insere = "(bind ?facto (assert (utilizador (incentivoMin " + incentivoMin + ")(estacaoFinal "+ fim.getEstacao() +")(velocidade " + velocidade +")(tempo 0.05)(distPercorrida 0)(distTotal " + distTotal +")(coordsAtuais " +
								inicio.getCoordX() + " " + inicio.getCoordY() + ")(coordsFim " + fim.getCoordX() + " " + fim.getCoordY() + "))))";
			 
			engineUtilizador.batch("src/spb/" + fileName);
			engineUtilizador.reset();
			engineUtilizador.executeCommand(insere);
			
			//inicializar este booleano a falso para que a estação final só seja notificada uma vez que o utilizador já ultrapassou 3/4 do percurso
			notifiqueiAgenteFinal = false;
			
			//inicializar este booleano a falso para que o utilizador não considere incentivos antes de ter ultrapassado 3/4 do percurso
			percurso75 = false;
			
			//inicializar este booleano para saber quando o utilizador chegou à estação final
			fimPercurso = false;
			
			//inicializar este booleano para saber se o utilizador deve enviar a mensagem à estação final a dizer que chegou
			//se o incentivo tiver sido aceite, a mensagem de confirmação já foi enviada
			incentivoAceite = false;
			
			
			//adicionar a estação inicial à lista de áreas em que ele se situa e notificar a estação que requisitou uma bicicleta
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.addReceiver(new AID(inicio.getEstacao(), AID.ISLOCALNAME));
			msg.setContent("bicicleta");
			send(msg);
			
			
			MessageTemplate mt1 = MessageTemplate.MatchContent("bicicleta");
			MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
			MessageTemplate mt3 = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
			MessageTemplate mt4 = MessageTemplate.and(mt1, mt2);
			MessageTemplate mt5 = MessageTemplate.and(mt1, mt3);
			MessageTemplate mt6 = MessageTemplate.or(mt4, mt5);
			
			ACLMessage  acl = blockingReceive(mt6);
			
			if(acl.getPerformative() == ACLMessage.AGREE)
			{
				// Adicionar estação inicial à lista de estações: o user encontra-se dentro da sua área
				entradas.put(inicio.getEstacao(),inicio);
				System.out.println("O utilizador " + getLocalName() + " alugou a bicicleta à estação "+ acl.getSender().getLocalName());
			}
			else if(acl.getPerformative() == ACLMessage.REFUSE)
			{
				System.out.println("Não havia bicicletas disponíveis na estação "+ acl.getSender().getLocalName() +" para o utilizador "+ getLocalName());
				this.doDelete();
			}
		}
		catch (Exception e) 
			{ e.printStackTrace();}
	}
	
	public class FimDoPercurso extends CyclicBehaviour
	{
		public void action()
		{
			if(incentivoAceite)
			{
				myAgent.doDelete();
			}
			else if(fimPercurso)
			{
				String estacao = "";
				QueryResult rs;
				ACLMessage msg;
				try 
				{
					rs = engineUtilizador.runQueryStar("procuraUtilizadores", new ValueVector());
					
					while(rs.next())
						estacao = rs.getString("e");
					
					//enviar a mensagem à estação final a dizer que chegou
					msg = new ACLMessage(ACLMessage.REQUEST);
					msg.addReceiver(new AID(estacao, AID.ISLOCALNAME));
					msg.setContent("entrega");
					send(msg);
					
					//receber a confirmação do incentivo
					
					MessageTemplate mt = MessageTemplate.MatchContent("entrega");

					msg = blockingReceive(mt);
					if(msg.getPerformative() == ACLMessage.AGREE)
					{
						myAgent.doDelete();
						System.out.println("O utilizador " + myAgent.getLocalName() + " entregou a bicicleta na estação " + msg.getSender().getLocalName());
					}
					else if(msg.getPerformative() == ACLMessage.REFUSE)
					{
						System.out.println("Eu sou o agente " + myAgent.getLocalName() + " e continuo à espera para entregar a bicicleta na estação " + msg.getSender().getLocalName());
						Thread.sleep(10000);
					}
					
				} catch (InterruptedException | JessException e) {
					e.printStackTrace();
				}
				
				
			}
		}
	}
	
	public class RecebeIncentivo extends CyclicBehaviour
	{
		public void action()
		{
			ACLMessage msg;
			String novaEstacao = "";
			Estacao e = null;
			float incentivo, incentivoMin;
			incentivoMin = 0.0f;
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			msg = receive(mt);
			if(msg != null)
			{
				if(percurso75)
				{
					incentivo = Float.parseFloat(msg.getContent());
					novaEstacao = msg.getSender().getLocalName();
					try 
					{
						//comparar o incentivo recebido ao minimo do utilizador
						QueryResult rs = engineUtilizador.runQueryStar("procuraUtilizadores", new ValueVector());
						
						while(rs.next())
							incentivoMin = rs.getFloat("i");
						
						//se o incentivo for maior, apaga este behaviour, muda a estação, muda as coordenadas da estação
						if(incentivoMin <= incentivo)
						{
							
							rs = engineEstacoes.runQueryStar("procuraEstacoes", new ValueVector());
							
							while(rs.next())
							{
								if(rs.getString("n").equals(novaEstacao))
								{
									e = new Estacao(rs.getString("n"), rs.getDouble("x"), rs.getDouble("y"), rs.getFloat("r"));
								}
							}
							
							//enviar a mensagem a confirmar o incentivo  
							msg = new ACLMessage(ACLMessage.REQUEST);
							msg.addReceiver(new AID(e.getEstacao(), AID.ISLOCALNAME));
							msg.setContent("incentivo aceite");
							send(msg);
							
							//receber a confirmação do incentivo
							MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
							MessageTemplate mt2 = MessageTemplate.MatchContent("lugar reservado");
							MessageTemplate mt3 = MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM);
							MessageTemplate mt4 = MessageTemplate.and(mt1, mt2);
							MessageTemplate mt5 = MessageTemplate.and(mt2, mt4);
							MessageTemplate mt6 = MessageTemplate.or(mt5, mt4);
							
							msg = blockingReceive(mt5);
							//se for possível depositar a bicicleta
							if(msg.getPerformative() == ACLMessage.CONFIRM)
							{
								// FALTA CALCULAR NOVA DISTÂNCIA FINAL
								String modify = "(modify ?facto (estacaoFinal " + e.getEstacao() +")(coordsFim " + e.getCoordX() +" " + e.getCoordY() +"))";
								engineUtilizador.executeCommand(modify);
								
								incentivoAceite = true;
								System.out.println("Incentivo de valor " + incentivo + " e enviado pela estação "+ msg.getSender().getLocalName() + "aceite pelo utilizador: " + myAgent.getLocalName());
								myAgent.removeBehaviour(this);
							}
							else if(msg.getPerformative() == ACLMessage.DISCONFIRM)
							{
								System.out.println("Incentivo recebido pelo utilizador "+ myAgent.getLocalName() + " não foi confirmado pela estação " + msg.getSender().getLocalName());
							}
						}
					}
					catch (JessException ex) 
					{
						ex.printStackTrace();
					}
				}
			}
		}
	}
	
	public class AtualizaPosicao extends TickerBehaviour
	{	
		public AtualizaPosicao(Agent a, long timeout)
		{
			super(a, timeout);
		}
		
		public void onTick()
		{	
			try 
			{

				List<String> aux = new ArrayList<>();
				String estacaoFinal;
				ACLMessage msg;
				double x0, x1, y0, y1, distTotal, distPercorrida, dist, tresQuartos, v, t;
				x0 = x1 = y0 = y1 = distTotal = distPercorrida = v = t = 0 ;
				estacaoFinal = "";
				
				//atualiza a posição
				QueryResult rs = engineUtilizador.runQueryStar("procuraUtilizadores", new ValueVector());
				//este ciclo só deve correr uma vez, visto que só há um utilizador no .clp
				while(rs.next())
				{
					x0 = rs.getDouble("x0");
					y0 = rs.getDouble("y0");
					x1 = rs.getDouble("x1");
					y1 = rs.getDouble("y1");
					v = rs.getInt("v");
					t = rs.getFloat("t");
					distTotal = rs.getDouble("dt");
					distPercorrida = rs.getDouble("dp");
					estacaoFinal = rs.getString("e");
					
					double dis = v*t;
					double novoX, novoY;
					double novaDist = distPercorrida + dis;
					if(novaDist > distTotal)
					{
						novaDist = distTotal;
						novoX = x1;
						novoY = y1;
					}
					else
					{
						novoX = x0 +  (dis * (x1 - x0))/ (distTotal - distPercorrida);
						novoY = y0 + (dis * (y1 - y0))/ (distTotal - distPercorrida);
					}

					engineUtilizador.executeCommand("(modify ?facto (distPercorrida " + novaDist + ")(coordsAtuais " + novoX + " " + novoY + "))");
				}
				
				//verificar se saiu da área de alguém
				for(Map.Entry<String, Estacao> e: entradas.entrySet())
				{
					
					dist = Math.sqrt(Math.pow(x0 - e.getValue().getCoordX(), 2)  + Math.pow(y0 - e.getValue().getCoordY(), 2));
					if(dist > e.getValue().getRaio())
					{
						System.out.println("O utilizador " + myAgent.getLocalName() +" saiu da área de " + e.getValue().getEstacao());
						aux.add(e.getKey());
						msg = new ACLMessage(ACLMessage.INFORM);
						msg.addReceiver(new AID(e.getValue().getEstacao(), AID.ISLOCALNAME));
						msg.setContent("out");
						send(msg);
					}
				}
				for(String e : aux)
					entradas.remove(e); 
	
				
				//verifica se entrou numa nova área e notifica-a de tal acontecimento
				for(Estacao e : estacoes)
				{
					dist = Math.sqrt(Math.pow(x0 - e.getCoordX(), 2)  + Math.pow(y0 - e.getCoordY(), 2));
					if(dist <= e.getRaio())
					{
	
						if(!entradas.containsKey(e.getEstacao()))
						{
							System.out.println("O utilizador " + myAgent.getLocalName() +" entrou na área de " + e.getEstacao());
							entradas.put(e.getEstacao(), new Estacao(e.getEstacao(), e.getCoordX(), e.getCoordY(), e.getRaio()));
							msg = new ACLMessage(ACLMessage.INFORM);
							msg.addReceiver(new AID(e.getEstacao(), AID.ISLOCALNAME));
							msg.setContent("in");
							send(msg);
						}
					}
				}
				
				//verifica se ultrapassou 3/4 do percurso e notifica-a de tal acontecimento
				tresQuartos = (3.0d * distTotal) / 4.0d;
				if(notifiqueiAgenteFinal == false && distPercorrida >= tresQuartos)
				{
					notifiqueiAgenteFinal = true;
					percurso75 = true;
					msg = new ACLMessage(ACLMessage.INFORM);
					msg.addReceiver(new AID(estacaoFinal, AID.ISLOCALNAME));
					msg.setContent("coords " + x0 + " " + y0); 
					send(msg);
				}
				
				if(distPercorrida >= distTotal)
				{
					fimPercurso = true;
					System.out.println("O utilizador " + myAgent.getLocalName() + " chegou ao fim do percurso.");
					myAgent.addBehaviour(new FimDoPercurso());
					myAgent.removeBehaviour(this);
				}
			} 
			catch (JessException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	protected void setup()
	{
		super.setup();
		initUtilizador();
		this.addBehaviour(new AtualizaPosicao(this, 1000));
		this.addBehaviour(new RecebeIncentivo());
	}
	
	protected void takeDown()
	{
		super.takeDown();
		String fileName = getAID().getLocalName() + ".clp";
		File f = new File("src/spb/", fileName);
		f.delete();
	}
	
}
