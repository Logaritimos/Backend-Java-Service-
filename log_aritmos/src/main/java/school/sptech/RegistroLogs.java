package school.sptech;

import java.time.LocalDateTime;

public class RegistroLogs {
    private Integer idLogs;
    private String categoria;
    private String descricao;
    private LocalDateTime dtHora;

    public RegistroLogs(String categoria, String descricao){
        this.categoria = categoria;
        this.descricao = descricao;
        this.dtHora = LocalDateTime.now();
    }
    public RegistroLogs(){}

    public Integer getIdLogs() {
        return idLogs;
    }

    public void setIdLogs(Integer idLogs){
        this.idLogs = idLogs;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getDtHora() {
        return dtHora;
    }

    public void setDtHora(LocalDateTime dtHora) {
        this.dtHora = dtHora;
    }

    public void exibirCabecalhos() {
        System.out.println("██╗      ██████╗  ██████╗  █████╗ ██████╗ ██╗████████╗███╗   ███╗ ██████╗ ███████╗");
        System.out.println("██║     ██╔═══██╗██╔════╝ ██╔══██╗██╔══██╗██║╚══██╔══╝████╗ ████║██╔═══██╗██╔════╝");
        System.out.println("██║     ██║   ██║██║  ███╗███████║██████╔╝██║   ██║   ██╔████╔██║██║   ██║███████╗");
        System.out.println("██║     ██║   ██║██║   ██║██╔══██║██╔██╔╝ ██║   ██║   ██║╚██╔╝██║██║   ██║╚════██║");
        System.out.println("███████╗╚██████╔╝╚██████╔╝██║  ██║██║╚██╗ ██║   ██║   ██║ ╚═╝ ██║╚██████╔╝███████║");
        System.out.println("╚══════╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝ ╚═╝ ╚═╝   ╚═╝   ╚═╝     ╚═╝ ╚═════╝ ╚══════╝");
    }
}
