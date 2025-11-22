package model;

import java.util.Date;

public class Cliente {

	private int idCliente;
    private String username;
    private String password;
    private String nome;
    private String cognome;
    private String CF;
    private String luogoNascita;
    private Date dataNascita;
    private String iban;

    // Oggetti collegati
    private Abbonamento abbonamento;  // null = nessun abbonamento
    private Pagamento pagamento;      // ultimo pagamento effettuato

    // ============================
    // COSTRUTTORI
    // ============================
    public Cliente() {
        // costruttore vuoto richiesto da vari framework / DAO
    }

    public Cliente(String username,
                   String password,
                   String nome,
                   String cognome,
                   String CF,
                   String luogoNascita,
                   Date dataNascita,
                   String iban) {

        this.username = username;
        this.password = password;
        this.nome = nome;
        this.cognome = cognome;
        this.CF = CF;
        this.luogoNascita = luogoNascita;
        this.dataNascita = dataNascita;
        this.iban = iban;
    }

    // ============================
    // GETTER & SETTER
    // ============================
    
    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public String getCF() { return CF; }
    public void setCF(String CF) { this.CF = CF; }

    public String getLuogoNascita() { return luogoNascita; }
    public void setLuogoNascita(String luogoNascita) { this.luogoNascita = luogoNascita; }

    public Date getDataNascita() { return dataNascita; }
    public void setDataNascita(Date dataNascita) { this.dataNascita = dataNascita; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public Abbonamento getAbbonamento() { return abbonamento; }
    public void setAbbonamento(Abbonamento abbonamento) { this.abbonamento = abbonamento; }

    public Pagamento getPagamento() { return pagamento; }
    public void setPagamento(Pagamento pagamento) { this.pagamento = pagamento; }

    // ============================
    // METODI DI COMODO DI BUSINESS
    // ============================

    /** Ritorna true se il cliente ha un abbonamento attivo (non nullo e non scaduto). */
    public boolean hasAbbonamentoAttivo() {
        if (abbonamento == null) {
            return false;
        }
        Date oggi = new Date();
        return abbonamento.getScadenza() == null
                || abbonamento.getScadenza().after(oggi);
    }

    /**
     * Sottoscrive un nuovo abbonamento, associandolo al cliente insieme
     * al pagamento effettuato (nel tuo flusso: schermata di pagamento → crea
     * Abbonamento + Pagamento → chiama questo metodo).
     */
    public void sottoscriviAbbonamento(Abbonamento nuovo, Pagamento pag) {
        this.abbonamento = nuovo;
        this.pagamento = pag;
    }

    /** Disdice l'abbonamento attuale (nel tuo flusso poi riapri la schermata scelta abbonamento). */
    public void disdiciAbbonamento() {
        this.abbonamento = null;
        // volendo puoi lasciare l'ultimo pagamento memorizzato
    }

    // I metodi Papyrus li lasciamo vuoti o li facciamo richiamare la logica esistente.
    public void loginApp(String username, String password) {
        // Gestito a livello di controller/DAO
    }

    public void vediAbbonamento() {
        // La GUI userà getAbbonamento() per mostrare i dati
    }

    public void prenotaConsulenza(Dipendente dip, Date data) {
        // Non implementato per questo progetto
    }

    /**
     * Metodo di comodo: crea un Pagamento a partire da importo/metodo
     * e lo associa al cliente (non interagisce col DB, è solo lato model).
     */
    public Pagamento effettuaPagamento(float importo, String metodo) {
        Pagamento p = new Pagamento(metodo, importo, new Date());
        this.pagamento = p;
        return p;
    }
}
