# Parser de Campos Lexicais (PCL)

Parser de campos lexicais. Esta classe foi desenvolvida para dar suporte ao trabalho de pesquisa de final de curso da Uninter em Maçonologia e Historia da Maçonaria, sob orientação do Prof. Alvaro Crovador. 

Este parser foi criado para buscar eficientemente uma lista de palavras/expressões em um arquivo de texto. Ele utiliza o algoritmo [Aho-Corasick](https://cran.r-project.org/web/packages/AhoCorasickTrie/AhoCorasickTrie.pdf) para realizar a busca em uma única passagem pelo texto.

## Objetivos do PCL 

O PCL visa realizar a contagem, em textos do *Corpus midiático* a quantidade de ocorrências de lexias presentes nos campos lexicais formados à partir dos documentos do *Corpus Eclesiástico* (textos e atos pontifícios condenatórios à Franco-Maçonaria). 

Esta contagem deve fornecer os totais de lexias individuais, bem como os totais agregados, valores que serão utilizados para a realização dos cálculos estatísticos que embasarão a análise quantitativa da pesquisa. 

## Requisitos funcionais

- recebe em entrada: lista de campos lexicais e lista de artigos a analisar
- identificar presença de palavras dos campos lexicais nos textos dos artigos
- retorna na saída: 
    - quantidade de ocorrências de cada palavra
    - estatisticas básicas sobre os textos
- normalização: 
    - etapa de pré-processamento:
        - excluir pontuação e sinais diacríticos
        - excluir "stop words" 
        - transformar letras em minúsculas
        - codificação em UTF-8 
- Algoritmo de pesquisa Aho-Corasick:
    - máxima eficiência na busca de multiplos padrões (palavras/expressões)
    (abordagem ingênua: varredura do texto por cada palavra; complexidade **O(N*M)**, *N=tamanho do texto, M=número total de caracteres nas palavras de busca*)
    - máquina de estados finitos (árvore de Trie com links de falha) a partir de todas as palavras de busca; processa o texto em uma única passagem, complexidade de tempo linear **O(N+M+Z)**, onde *Z=número de ocorrências encontradas*
- busca deve ser "case insensitive"
- busca deve encontrar palavras inteiras; não usar a abordagem de lemmificação da pesquisa
- as listas de campos lexicais e os textos dos artigos precisam poder receber comentários (sinal # na primeira coluna); linhas comentadas não são processadas pelo programa
- apresentar os resultados finais em ordem decrescente das quantidades encontradas dos termos; para termos a mesma quantidade, ordenar alfabeticamente
- tratar variações de termos apresentados na lista e totalizar todas as ocorrências sob seu termo base. Múltiplos termos de busca (variantes) devem ser contados sob único rótulo (termo base). A estrutura das entradas dos termos na lista dos campos lexicais é a seguinte: 
```
[TERMO A, TERMO B, TERMO C], onde
    TERMO A -> termo base, ou raíz
    TERMO B, TERMO C -> termos variantes 
```
- toda contagem de termos variantes será agregada à contagem do termo raiz
- Totalizadores: 
```
Quantidade de termos presentes na lista de campos lexicos 
Quantidade de palavras no texto do corpus
Total de ocorrências encontradas no texto
Número de termos únicos: termos, localizados no texto, ou seja, ocorrências > 0.
Lista de palavras com as respectivas quantidades de ocorrência, ordenadas por quantidade e alfabeticamente
```

## Funcionamento

A partir de de um conjunto de arquivos de campos lexicais e outro conjunto de arquivos de textos de artigo, o programa realiza uma etapa de pré-processamento, onde o texto é canonicalizado (conversao de letras a minúsculas, supressão de pontuação e sinais diacríticos, remoção de "stop words"), gerando assim um payload de trabalho. 

## Constituição dos Corpora Documentais 

- Corpus Eclesiástico: Atos pontifícios sobre a Franco-Maçonaria
- Corpus Midiático: Artigos da imprensa quebequense sobre a Franco-Maçonaria 

### **Nomenclatura**

**Textos de atos pontifícios:**

    [código da língua]-[iniciais do nome do texto]-[ano de publicação do texto]

Ex: 

`FR-IEA-1738` - Texto em francês de `In Eminenti Apostolatus`, publicado em 1738. 

`FR-HG-1884` - Texto em francês de `Humanus Genus`, publicado em 1884.

### **Metadados**

Os metadados auxiliam na identificação e na referência dos textos utilizados no trabalho. Para cada corpus diferente, os campos de metadados se adaptam. 


Corpus Eclesiástico

Os metadados do corpus são o título, a data de publicação, o Papa autor do documento, a lingua na qual o documento está redigido e a fonte do documento. A seguir encontra-s o exemplo dos metadados da carta encíclica *Humanum Genus*: 

    #
    # Título : Humanum Genus
    # Data   : 20/04/1884
    # Autor  : Leão XIII
    # Língua : Francês
    # Fonte  : https://www.vatican.va/content/leo-xiii/la/encyclicals/documents/hf_l-xiii_enc_18840420_humanum-genus.html
    #


