package spb;

public class Estacao {
	private String estacao;
	private double coordX;
	private double coordY;
	private float raio;
	
	public Estacao(String e, double x, double y, float r)
	{
		estacao = e;
		coordX = x;
		coordY = y;
		raio = r;
	}
	
	public String getEstacao()
	{
		return estacao;
	}
	
	public double getCoordX()
	{
		return coordX;
	}
	
	public double getCoordY()
	{
		return coordY;
	}
	
	public float getRaio()
	{
		return raio;
	}
}
