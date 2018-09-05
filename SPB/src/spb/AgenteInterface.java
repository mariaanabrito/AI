package spb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgenteInterface extends Agent{
	
	private AID[] estacoesAgentes;
	private DFAgentDescription[] estacoes;
	private Map<AID, InfoEstacao> info;
	JFrame jAluguer = new JFrame(); 
	JFrame jEntradas = new JFrame(); 
	JFrame jDevolucoes = new JFrame(); 
	JFrame jBicicletas = new JFrame(); 
	JFrame jOcupacao = new JFrame();
	JFrame jPedidos = new JFrame(); 
	
	private void initDF()
	{
	
		try
		{	
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			template.addServices(sd);
			estacoes = DFService.search(this, template);
			estacoesAgentes = new AID[estacoes.length];
			
			for(int i = 0; i < estacoes.length; ++i)
			{
				estacoesAgentes[i] = estacoes[i].getName();
				info.put(estacoesAgentes[i], new InfoEstacao());
			}
		}
		catch(FIPAException e)
		{
			e.printStackTrace();
		}
		
		
	}
	
	public class EstatisticasEstacao extends SimpleBehaviour
	{
		private AID aid;
		private boolean done;
		public EstatisticasEstacao(AID a)
		{
			aid = a;
			done = false;
		}
		public void action()
		{
			ACLMessage acl  = new ACLMessage(ACLMessage.REQUEST);
			acl.addReceiver(aid);
			acl.setContent("stats");
			send(acl);
			
			
			MessageTemplate mt = MessageTemplate.MatchSender(aid);
			ACLMessage msg = receive(mt);
			
			if(msg != null)
			{
				//parse da string
				//string: "alugueres devolucoes entradas biclasarmazenadas ocupacao numPedidos"
				// separadas por um espaço
				
				String content = msg.getContent();
				String [] comp = content.split(" ");
				InfoEstacao ie = info.get(aid);
				
				ie.setNumAlugueres(Integer.parseInt(comp[0]));
				ie.setNumDevolucoes(Integer.parseInt(comp[1]));
				ie.setNumEntradas(Integer.parseInt(comp[2]));
				ie.setNumBicicletas(Integer.parseInt(comp[3]));
				ie.setOcupacao(Float.parseFloat(comp[4]));
				ie.setNumPedidos(Integer.parseInt(comp[5]));
				
				info.remove(aid);
				info.put(aid, ie);
				done = true;
				
			}
		}

		@Override
		public boolean done() {
			return done;
		}
	}
	
	public void estatisticasInterfaceGrafica()
	{
		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		double width = screenSize.getWidth() - 20.0d;
		double height = screenSize.getHeight() - 20.0d;
		
		DefaultCategoryDataset datasetAluguer = new DefaultCategoryDataset();
		DefaultCategoryDataset datasetEntradas = new DefaultCategoryDataset();
		DefaultCategoryDataset datasetDevolucoes = new DefaultCategoryDataset();
		DefaultCategoryDataset datasetBicicletas = new DefaultCategoryDataset();
		DefaultCategoryDataset datasetOcupacao = new DefaultCategoryDataset();
		DefaultCategoryDataset datasetPedidos = new DefaultCategoryDataset();
		
		for(Map.Entry<AID, InfoEstacao> entry: info.entrySet())
		{
			System.out.println("Estação : " + entry.getKey().getLocalName() + " " + entry.getValue().toString());
			datasetAluguer.setValue(entry.getValue().getNumAlugueres(), "#Alugueres", entry.getKey().getLocalName().replaceAll("[^\\d.]", ""));
			datasetEntradas.setValue(entry.getValue().getNumEntradas(), "#Entradas", entry.getKey().getLocalName().replaceAll("[^\\d.]", ""));
			datasetDevolucoes.setValue(entry.getValue().getNumDevolucoes(), "#Devoluções", entry.getKey().getLocalName().replaceAll("[^\\d.]", ""));
			datasetBicicletas.setValue(entry.getValue().getNumBicicletas(), "#Bicicletas", entry.getKey().getLocalName().replaceAll("[^\\d.]", ""));
			datasetOcupacao.setValue(entry.getValue().getOcupacao(), "% ocupação", entry.getKey().getLocalName().replaceAll("[^\\d.]", ""));
			datasetPedidos.setValue(entry.getValue().getNumPedidos(), "#Pedidos", entry.getKey().getLocalName().replaceAll("[^\\d.]", ""));
			  
		}
		
		JPanel jPanel3 = new JPanel();
		jPanel3.removeAll();
	    jPanel3.revalidate(); // This removes the old chart
		JFreeChart chartAluguer = ChartFactory.createBarChart("Número de alugueres","Estação", "#Alugueres", datasetAluguer, 
				   											PlotOrientation.VERTICAL, false,true, false);
		chartAluguer.setBackgroundPaint(Color.white);
		chartAluguer.getTitle().setPaint(Color.blue); 
		CategoryPlot p = chartAluguer.getCategoryPlot(); 
		p.setRangeGridlinePaint(Color.red); 
		ChartPanel frame1 = new ChartPanel(chartAluguer);
		jPanel3.setLayout(new BorderLayout()); 
	    jPanel3.add(frame1); 
	    jPanel3.repaint();
		jPanel3.add(frame1);
		
		
		JPanel jPanel2 = new JPanel();
		jPanel2.removeAll();
	    jPanel2.revalidate(); // This removes the old chart
		JFreeChart chartEntradas = ChartFactory.createBarChart("Número de entradas na área da estação","Estação", "#Entradas", datasetEntradas, 
					PlotOrientation.VERTICAL, false,true, false);
		chartEntradas.setBackgroundPaint(Color.white);
		chartEntradas.getTitle().setPaint(Color.blue); 
		CategoryPlot p1 = chartEntradas.getCategoryPlot(); 
		p1.setRangeGridlinePaint(Color.red);
		ChartPanel frame2 =new ChartPanel(chartEntradas);
		jPanel2.setLayout(new BorderLayout()); 
	    jPanel2.add(frame2); 
	    jPanel2.repaint();
		
		
		
		JPanel jPanel1 = new JPanel();
		jPanel1.removeAll();
	    jPanel1.revalidate(); // This removes the old chart
		JFreeChart chartDevolucoes = ChartFactory.createBarChart("Número de devoluções","Estação", "#Devoluções", datasetDevolucoes, 
				PlotOrientation.VERTICAL, false,true, false);
		chartDevolucoes.setBackgroundPaint(Color.white);
		chartDevolucoes.getTitle().setPaint(Color.blue); 
		p1 = chartDevolucoes.getCategoryPlot(); 
		p1.setRangeGridlinePaint(Color.red);
		ChartPanel frame3 = new ChartPanel(chartDevolucoes);
	    jPanel1.setLayout(new BorderLayout()); 
	    jPanel1.add(frame3); 
	    jPanel1.repaint();
		
		
	    JPanel jPanel4 = new JPanel();
		jPanel4.removeAll();
	    jPanel4.revalidate();
		JFreeChart chartBicicletas = ChartFactory.createBarChart("Número de bicicletas armazenadas","Estação", "#Bicicletas", datasetBicicletas, 
				PlotOrientation.VERTICAL, false,true, false);
		chartBicicletas.setBackgroundPaint(Color.white);
		chartBicicletas.getTitle().setPaint(Color.blue); 
		p1 = chartBicicletas.getCategoryPlot(); 
		p1.setRangeGridlinePaint(Color.red);
		ChartPanel frame4 =new ChartPanel(chartBicicletas);
		jPanel4.setLayout(new BorderLayout()); 
	    jPanel4.add(frame4); 
	    jPanel4.repaint();
	    
	    JPanel jPanel5 = new JPanel();
		jPanel5.removeAll();
	    jPanel5.revalidate();
		JFreeChart chartOcupacao = ChartFactory.createBarChart("% de ocupação","Estação", "% ocupação", datasetOcupacao, 
				PlotOrientation.VERTICAL, false,true, false);
		chartOcupacao.setBackgroundPaint(Color.white);
		chartOcupacao.getTitle().setPaint(Color.blue); 
		p1 = chartOcupacao.getCategoryPlot(); 
		p1.setRangeGridlinePaint(Color.red);
		ChartPanel frame5 =new ChartPanel(chartOcupacao);
		jPanel5.setLayout(new BorderLayout()); 
	    jPanel5.add(frame5); 
	    jPanel5.repaint();
	    
	    JPanel jPanel6 = new JPanel();
		jPanel6.removeAll();
	    jPanel6.revalidate();
		JFreeChart chartPedidos = ChartFactory.createBarChart("Nº de pedidos de bicicletas","Estação", "#Pedidos", datasetPedidos, 
				PlotOrientation.VERTICAL, false,true, false);
		chartPedidos.setBackgroundPaint(Color.white);
		chartPedidos.getTitle().setPaint(Color.blue); 
		p1 = chartPedidos.getCategoryPlot(); 
		p1.setRangeGridlinePaint(Color.red);
		ChartPanel frame6 =new ChartPanel(chartPedidos);
		jPanel6.setLayout(new BorderLayout()); 
	    jPanel6.add(frame6); 
	    jPanel6.repaint();
		
	    
		jAluguer.getContentPane().add(jPanel3);
		jAluguer.setVisible(true);
		int w = (int) width/3;
		int h = (int) height/2;
		jAluguer.setSize((int) w,(int)h);
		
		
		jEntradas.getContentPane().add(jPanel2);
		jEntradas.setVisible(true);
		jEntradas.setSize((int) w,(int)h);
		jEntradas.setLocation(w, 0);

		
		jDevolucoes.getContentPane().add(jPanel1);
		jDevolucoes.setVisible(true);
		jDevolucoes.setSize((int) w,(int)h);
		jDevolucoes.setLocation(w * 2, 0);
		
		
		jBicicletas.getContentPane().add(jPanel4);
		jBicicletas.setVisible(true);
		jBicicletas.setSize((int) w,(int)h);
		jBicicletas.setLocation(0, h);
		
		
		jOcupacao.getContentPane().add(jPanel5);
		jOcupacao.setVisible(true);
		jOcupacao.setSize((int) w,(int)h);
		jOcupacao.setLocation(w, h);
		
	
		jPedidos.getContentPane().add(jPanel6);
		jPedidos.setVisible(true);
		jPedidos.setSize((int) w,(int)h);
		jPedidos.setLocation(2 *w, h);
		
	}
	
	public class PedeEstatisticas extends TickerBehaviour
	{
		public PedeEstatisticas(Agent a, long timeout)
		{
			super(a, timeout);
		}
		
		public void onTick()
		{	
			ParallelBehaviour estatisticas =  new ParallelBehaviour(myAgent, ParallelBehaviour.WHEN_ALL){
				public int onEnd()
				{
					jAluguer.setTitle("Alugueres");
					jEntradas.setTitle("Entradas");
					jDevolucoes.setTitle("Devoluções");
					jBicicletas.setTitle("Armazenamento");
					jOcupacao.setTitle("Ocupação");
					jPedidos.setTitle("Pedidos");
					estatisticasInterfaceGrafica();
					return 1;
				}
			};
			
			for(int i = 0; i < estacoesAgentes.length; i++)
			{
				estatisticas.addSubBehaviour(new EstatisticasEstacao(estacoesAgentes[i]));
			}
			
			myAgent.addBehaviour(estatisticas);
		}
	}

	protected void setup()
	{
		super.setup();
		Comparator<AID> porOrdemAlfabeticaEstacao = 
				(AID a1, AID a2) -> Integer.parseInt(a1.getLocalName().replaceAll("[^\\d.]", "")) - Integer.parseInt(a2.getLocalName().replaceAll("[^\\d.]", ""));
		info = new TreeMap<AID, InfoEstacao>(porOrdemAlfabeticaEstacao);
		initDF();
		addBehaviour(new PedeEstatisticas(this,10000));	
	}
}
