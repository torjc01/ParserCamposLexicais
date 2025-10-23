# Parser de Campos Lexicais (PCL)

Parser de campos lexicais. Esta classe foi desenvolvida para dar suporte ao trabalho de pesquisa de final de curso da Uninter em Maçonologia e Historia da Maçonaria, sob orientação do Prof. Alvaro Crovador. 

## Objetivos do PCL 

O PCL visa realizar a contagem, em textos do *Corpus midiático* a quantidade de ocorrências de lexias presentes nos campos lexicais formados à partir dos documentos do *Corpus Eclesiástico* (textos e atos pontifícios condenatórios à Franco-Maçonaria). 

Esta contagem deve fornecer os totais de lexias individuais, bem como os totais agregados, valores que serão utilizados para a realização dos cálculos estatísticos que embasarão a análise quantitativa da pesquisa. 

## Funcionamento

A partir de de um conjunto de arquivos de campos lexicais e outro conjunto de arquivos de textos de artigo, o programa realiza uma etapa de pré-processamento, onde o texto é canonicalizado (conversao de letras a minúsculas, supressão de pontuação e sinais diacríticos, remoção de "stop words"), gerando assim um payload de trabalho. 


