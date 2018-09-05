package spb;

public class InfoEstacao {
	private int numAlugueres, numDevolucoes, numEntradas, numBicicletas, numPedidos;
	private float ocupacao;
	
	public InfoEstacao()
	{
		numAlugueres = 0;
		numDevolucoes = 0;
		numEntradas = 0;
		numBicicletas = 0;
		ocupacao = 0.0f;
		numPedidos = 0;
	}
	
	public InfoEstacao(int a, int d, int e, int b, float o, int p)
	{
		numAlugueres = a;
		numDevolucoes = d;
		numEntradas = e;
		numBicicletas = b;
		ocupacao = o;
		numPedidos = p;
	}

	public int getNumPedidos() {
		return numPedidos;
	}

	public void setNumPedidos(int numPedidos) {
		this.numPedidos = numPedidos;
	}

	public int getNumAlugueres() {
		return numAlugueres;
	}

	public void setNumAlugueres(int numAlugueres) {
		this.numAlugueres = numAlugueres;
	}

	public int getNumDevolucoes() {
		return numDevolucoes;
	}

	public void setNumDevolucoes(int numDevolucoes) {
		this.numDevolucoes = numDevolucoes;
	}

	public int getNumEntradas() {
		return numEntradas;
	}

	public void setNumEntradas(int numEntradas) {
		this.numEntradas = numEntradas;
	}

	public int getNumBicicletas() {
		return numBicicletas;
	}

	public void setNumBicicletas(int numBicicletas) {
		this.numBicicletas = numBicicletas;
	}

	public float getOcupacao() {
		return ocupacao;
	}

	public void setOcupacao(float ocupacao) {
		this.ocupacao = ocupacao;
	}

	@Override
	public String toString() {
		return "Info: numAlugueres=" + numAlugueres + ", numDevolucoes=" + numDevolucoes + ", numEntradas="
				+ numEntradas + ", numBicicletas=" + numBicicletas + ", ocupacao=" + ocupacao +", numPedidos=" + numPedidos;
	}
}
