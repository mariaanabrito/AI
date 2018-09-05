(deftemplate estacoes
	(slot nome)
	(slot capacidadeMax)
	(slot raio)
	(multislot coords)
)
(deffacts factos
	(estacoes (nome AE1)(capacidadeMax 10)(raio 2.5)(coords 16 9))
	(estacoes (nome AE2)(capacidadeMax 10)(raio 2)(coords 10 9))
	(estacoes (nome AE3)(capacidadeMax 10)(raio 2.3)(coords 8 4))
	(estacoes (nome AE4)(capacidadeMax 10)(raio 2.5)(coords 8 7))
	(estacoes (nome AE5)(capacidadeMax 10)(raio 3)(coords 11 5))
	(estacoes (nome AE6)(capacidadeMax 10)(raio 2.5)(coords 11 2))
	(estacoes (nome AE7)(capacidadeMax 10)(raio 2.3)(coords 13 9))
	(estacoes (nome AE8)(capacidadeMax 10)(raio 3)(coords 15 6))
	(estacoes (nome AE9)(capacidadeMax 10)(raio 2.4)(coords 14 3))
	(estacoes (nome AE10)(capacidadeMax 10)(raio 2)(coords 15 4))
)

(defquery procuraEstacoes
	(estacoes (nome ?n)(raio ?r)(capacidadeMax ?max)(coords ?x ?y))
)