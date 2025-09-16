package org.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GeradorPaginaEditorial {

    private final LocalDate dataDeGeracao;

    // O construtor recebe a data para tornar a página dinâmica
    public GeradorPaginaEditorial(LocalDate dataDeGeracao) {
        this.dataDeGeracao = dataDeGeracao;
    }

    // Este método faz todo o trabalho de montar e retornar o HTML
    public String gerarHtml() {
        // Formata a data para um padrão amigável em português
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
        String dataFormatada = dataDeGeracao.format(formatador);

        StringBuilder editorialHtml = new StringBuilder();
        editorialHtml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\">")
                .append("<head>")
                .append("<title>Editorial</title>")
                .append("<style type=\"text/css\">")
                .append("body { font-family: sans-serif; margin: 5% 10%; line-height: 1.5; text-align: center; }")
                .append("h1 { font-size: 1.4em; margin-bottom: 2px; }")
                .append("p { margin: 2px 0; }")
                .append(".subtitulo { font-size: 0.9em; margin-bottom: 2em; }")
                .append(".info-block { margin-top: 2em; margin-bottom: 2em; }")
                .append(".responsabilidade { font-size: 0.9em; }")
                .append(".nome-cargo { margin-top: 2em; }")
                .append(".nome { font-weight: bold; }")
                .append(".cargo { font-style: italic; font-size: 0.9em; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")

                .append("<h1>EDITORIAL</h1><br/>")

                .append("<p class=\"subtitulo\">SÃO PAULO – 2025 – Ano 70 – Edição nº ___ <br/></p>")
                .append("<div class=\"info-block\">")
                .append("<p> Lei 5.075 de 31 de outubro de 1956 <br/>")
                .append("DOSP - DIÁRIO OFICIAL DE SÃO PAULO <br/>")
                .append("Decreto 62.177 de 24 de fevereiro de 2023 </p>")
                .append("</div>")
                .append("<p>disponível em: <a href=\"http://diariooficial.prefeitura.sp.gov.br\">http://diariooficial.prefeitura.sp.gov.br</a></p>")

                .append("<p><strong>Prefeitura de São Paulo</strong></p>")

                .append("<div class=\"info-block responsabilidade\">")
                .append("<p><strong>Responsabilidade editorial:</strong> Arquivo Público Municipal “Jornalista Paulo Roberto Dutra” - ARQUIP<br/>")
                .append("<strong>Coordenador:</strong> Darcio Gomes<br/>")
                .append("<strong>Jornalista responsável:</strong> Angelo Antônio Tibúrcio Mota – Mtb: 73656/SP</p>")
                .append("</div>")

                // ...
                .append("<div class=\"nome-cargo\">")
                .append("<p><span class=\"nome\">RICARDO NUNES</span><br/>") // Usamos <span> e <br/>
                .append("<span class=\"cargo\">Prefeito de São Paulo</span></p>")
                .append("</div>")

                .append("<div class=\"nome-cargo\">")
                .append("<p><span class=\"nome\">MARCELA ARRUDA</span><br/>")
                .append("<span class=\"cargo\">Secretária de Gestão</span></p>")
                .append("</div>")

                .append("<div class=\"nome-cargo\">")
                .append("<p><span class=\"nome\">RICARDO TEIXEIRA</span><br/>")
                .append("<span class=\"cargo\">Presidente da Câmara Municipal</span></p>")
                .append("</div>")

                .append("</body>")
                .append("</html>");

        return editorialHtml.toString();
    }
}