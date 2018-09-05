package spb;

public class Utilizador {
	private String nome;
	private float x, y;
	
	public Utilizador(String n, float x, float y)
	{
		nome = n;
		this.x = x;
		this.y = y;
	}
	
	public String getNome()
	{
		return nome;
	}
	
	public float getX()
	{
		return x;
	}
	
	public float getY()
	{
		return y;
	}
}
