package org.example;

import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConversorEpub {

    public static void main(String[] args) {

        Map<String, String> mapaDeCapas = new HashMap<>();
        mapaDeCapas.put("atos do executivo", "2.png");
        mapaDeCapas.put("concursos", "3.png");
        mapaDeCapas.put("editais", "4.png");
        mapaDeCapas.put("negócios", "5.png");
        mapaDeCapas.put("servidores", "6.png");
        mapaDeCapas.put("atos da cmsp", "7.png");
        mapaDeCapas.put("atos do tcm-sp", "8.png");


        try {
            // 1. Inicializa o objeto EPUB
            Book livro = new Book();
            Metadata metadata = livro.getMetadata();
            metadata.addTitle("Diário Oficial - Edição de 08 de Agosto de 2025");
            metadata.addAuthor(new Author("Gerador", "Automático"));

            // 2. Adiciona a Capa Principal do Jornal (1.png)
            File arquivoCapaPrincipal = new File("C:\\Users\\x396757\\OneDrive - rede.sp\\Área de Trabalho\\ToEpub\\ToEpub\\src\\main\\resources\\Capas\\1.png");
            if (arquivoCapaPrincipal.exists()) {
                Resource coverResource = new Resource(new FileInputStream(arquivoCapaPrincipal), "cover.png");
                livro.setCoverImage(coverResource);
                System.out.println("Capa principal '1.png' adicionada aos metdados.");

                String coverPageContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
                        "<head><title>Capa</title><style type=\"text/css\"> body { margin: 0; padding: 0; text-align: center; } img { max-width: 100%; height: 100vh; object-fit: contain; } </style></head>" +
                        "<body>" +
                        "<img src=\"" + coverResource.getHref() + "\" alt=\"Capa do Livro\"/>" +
                        "</body>" +
                        "</html>";
                Resource coverPageResource = new Resource(coverPageContent.getBytes(StandardCharsets.UTF_8), "cover.xhtml");
                livro.addSection("Capa Principal", coverPageResource);
                System.out.println("Página de capa principal (cover.xhtml) adicionada.");
            } else {
                System.out.println("\n Aviso: Arquivo da capa principal '1.png' não encontrado.");
            }

            // 3. Carrega e limpa o HTML
            File htmlInput = new File("C:\\Users\\x396757\\OneDrive - rede.sp\\Área de Trabalho\\ToEpub\\ToEpub\\src\\main\\resources\\diario10.html");
            Document doc = Jsoup.parse(htmlInput, "UTF-8", "");

            Safelist safelist = Safelist.relaxed()
                    .addTags("figure", "span", "table", "tbody", "tr", "td", "div", "ul", "li", "a") // Adicionei tags para o sumário
                    .addAttributes(":all", "style", "class", "id") // Adicionei 'id'
                    .addAttributes("a", "href") // Adicionei 'href' para os links
                    .addAttributes("img", "src")
                    .addProtocols("img", "src", "data");
            doc = new Cleaner(safelist).clean(doc);
            doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).charset(StandardCharsets.UTF_8);

            // 4. Extrai o CSS
            Resource cssResource = null;
            Element styleTag = doc.selectFirst("style");
            if (styleTag != null) {
                cssResource = new Resource(styleTag.html().getBytes(StandardCharsets.UTF_8), "estilos.css");
                livro.addResource(cssResource);
                System.out.println("CSS interno extraído com sucesso.");
            }

            // 5. Adiciona IDs únicos para todos os cabeçalhos para os Sumários
            Elements allHeadings = doc.select("h1, h2");
            int headingIndex = 0;
            for (Element heading : allHeadings) {
                String id = normalizarNome(heading.text()) + "-" + (headingIndex++);
                heading.attr("id", id);
            }

            // 6. Divide o HTML em seções baseadas nos H1s e processa cada uma
            Elements sectionsH1 = doc.select("h1");
            TableOfContents toc = livro.getTableOfContents();
            int sectionCounter = 1;

            System.out.println("Iniciando processamento de " + sectionsH1.size() + " seções...");

            for (Element h1 : sectionsH1) {
                System.out.println("\n--- Processando Seção " + sectionCounter + ": " + h1.text() + " ---");

                // 6.1. Adiciona a CAPA DA SEÇÃO de forma dinâmica
                String h1TextoNormalizado = h1.text().toLowerCase(); // Pega o título do H1 e converte para minúsculas
                String nomeArquivoCapa = null;

// Procura no mapa se algum texto chave corresponde ao título da seção
                for (Map.Entry<String, String> entry : mapaDeCapas.entrySet()) {
                    if (h1TextoNormalizado.contains(entry.getKey())) {
                        nomeArquivoCapa = entry.getValue();
                        break; // Encontrou a capa, pode parar de procurar
                    }
                }

// Se encontrou um nome de arquivo no mapa, tenta adicionar a capa
                if (nomeArquivoCapa != null) {
                    String basePath = "C:\\Users\\x396757\\OneDrive - rede.sp\\Área de Trabalho\\ToEpub\\ToEpub\\src\\main\\resources\\Capas\\";
                    File arquivoCapaSecao = new File(basePath + nomeArquivoCapa);

                    if (arquivoCapaSecao.exists()) {
                        String epubCapaHref = "capa_secao_" + sectionCounter + ".png"; // Nome único dentro do epub
                        Resource capaSecaoResource = new Resource(new FileInputStream(arquivoCapaSecao), epubCapaHref);
                        livro.addResource(capaSecaoResource);

                        String capaSecaoPageContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
                                "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
                                "<head><title>Capa da Seção</title><style type=\"text...\">...</style></head>" +
                                "<body><img src=\"" + epubCapaHref + "\" alt=\"Capa da Seção\"/></body></html>";

                        Resource capaSecaoPageResource = new Resource(capaSecaoPageContent.getBytes(StandardCharsets.UTF_8), "pagina_capa_secao_" + sectionCounter + ".xhtml");
                        livro.addResource(capaSecaoPageResource);
                        livro.getSpine().addResource(capaSecaoPageResource);
                        System.out.println("Capa '" + nomeArquivoCapa + "' adicionada para a seção '" + h1.text() + "'.");
                    } else {
                        System.out.println("Aviso: Capa '" + nomeArquivoCapa + "' mapeada, mas arquivo não encontrado.");
                    }
                } else {
                    System.out.println("Aviso: Nenhuma capa mapeada para a seção '" + h1.text() + "'.");
                }

                // 6.2. Extrai o CONTEÚDO DA SEÇÃO
                StringBuilder sectionHtmlBuilder = new StringBuilder();
                sectionHtmlBuilder.append(h1.outerHtml());
                Element nextElement = h1.nextElementSibling();
                while (nextElement != null && !nextElement.tagName().equalsIgnoreCase("h1")) {
                    sectionHtmlBuilder.append(nextElement.outerHtml());
                    nextElement = nextElement.nextElementSibling();
                }

                // ===== INÍCIO DA MODIFICAÇÃO: CRIA E INJETA O SUMÁRIO DA SEÇÃO =====

                // 6.3. Analisa o HTML da seção para encontrar os H2s
                Document sectionDoc = Jsoup.parseBodyFragment(sectionHtmlBuilder.toString());
                Elements h2sInSection = sectionDoc.select("h2");
                sectionDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

                // 6.4. Se houver H2s, cria o HTML do sumário
                if (!h2sInSection.isEmpty()) {
                    System.out.println("Gerando sumário interno para " + h2sInSection.size() + " subtítulos (H2).");
                    StringBuilder miniTocHtml = new StringBuilder();
                    // Adicionamos uma classe para o título para poder estilizá-lo
                    miniTocHtml.append("<div class=\"sumario-secao\">")
                            .append("<p class=\"sumario-titulo\">Sumário da seção:</p>") // <-- TÍTULO ADICIONADO
                            .append("<ul>\n");

                    for (Element h2 : h2sInSection) {
                        miniTocHtml.append("<li><a href=\"#").append(h2.id()).append("\">")
                                .append(h2.text()).append("</a></li>\n");
                    }
                    miniTocHtml.append("</ul></div>");

                    // 6.5. Injeta o sumário logo após o H1 dentro do documento da seção
                    Element h1InSec = sectionDoc.selectFirst("h1");
                    if (h1InSec != null) {
                        h1InSec.after(miniTocHtml.toString());
                    }
                }
                // ===== FIM DA MODIFICAÇÃO =====

                // 6.6. Cria a página XHTML para o CONTEÚDO DA SEÇÃO (agora com o sumário injetado)
                StringBuilder xhtmlContent = new StringBuilder();
                xhtmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                        .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n")
                        .append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>")
                        .append("<title>").append(h1.text()).append("</title>");
                if (cssResource != null) {
                    xhtmlContent.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(cssResource.getHref()).append("\" />");
                }
                // Adicionando um estilo básico para o sumário da seção
                xhtmlContent.append("<style>" +
                        "    .sumario-secao { " +
                        "    column-count: 2;\n" +
                        "    column-rule: 1px solid #434444;\n" +
                        "    background-color: #f5f5f5;\n" +
                        "    padding: 20px;\n" +
                        "    font-size: 12px;\n" +
                        "    border-radius: 8px;\n" +
                        "    border: 1px solid #e0e0e0;\n" +
                        "    margin: 25px 0;\n }" +

                        "    .sumario-secao ul { " +
                        "    list-style-type: none; \n" +
                        "    padding-left: 0; \n" +
                        "    margin: 0; } " +

                        "    .sumario-titulo { " +
                        "     font-weight: bold; " +
                        "     font-size: 1.1em; " +
                        "     margin-top: 0; " +
                        "     margin-bottom: 15px; " +
                        "     padding-bottom: 10px; " +
                        "     border-bottom: 1px solid #d0d0d0; " +
                        "     column-span: all; " +
                        "    } " +

                        "    .sumario-secao li { " +
                        "    padding-bottom: 12px; } " +

                        "    .sumario-secao li a { " +
                        "    text-decoration: none;" +
                        "    color: #424242; font-size: 0.95em; } " +

                        "    .sumario-secao li a:hover { " +
                        "    color: #000000; " +
                        "    text-decoration: underline; } " +
                        "</style>");
                xhtmlContent.append("</head>\n<body>\n")
                        // Usa o HTML modificado do sectionDoc que agora contém o sumário
                        .append(sectionDoc.body().html())
                        .append("</body>\n</html>");

                String secaoHref = "secao_" + sectionCounter + ".xhtml";
                Resource secaoResource = new Resource(xhtmlContent.toString().getBytes(StandardCharsets.UTF_8), secaoHref);
                livro.addResource(secaoResource);
// Passo 2: Adiciona o arquivo XHTML da seção à ordem de leitura (espinha).
                livro.getSpine().addResource(secaoResource);
                System.out.println("Conteúdo da seção adicionado como '" + secaoHref + "'.");

// Passo 3: Cria a ÚNICA entrada para o H1 no sumário.
                TOCReference h1Ref = new TOCReference(h1.text(), secaoResource, h1.id());
                toc.addTOCReference(h1Ref);
                System.out.println("TOC Principal: Adicionado H1 -> " + h1.text());

                for (Element h2 : h2sInSection) { // Reutiliza os H2s já encontrados
                    // SÓ ADICIONA O H2 SE O TEXTO DELE FOR DIFERENTE DO H1
                    if (!h2.text().equalsIgnoreCase(h1.text())) {
                        h1Ref.getChildren().add(new TOCReference(h2.text(), secaoResource, h2.attr("id")));
                        System.out.println("TOC Principal: Adicionado H2 -> " + h2.text());
                    }
                }

                sectionCounter++;
            }

            // 7. Escreve o arquivo EPUB
            EpubWriter epubWriter = new EpubWriter();
            epubWriter.write(livro, new FileOutputStream("jornal_oficial_final.epub"));
            System.out.println("\nEPUB gerado com sucesso: jornal_oficial_final.epub");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String normalizarNome(String texto) {
        return texto.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}