package it.csi.agrc;
import it.csi.cuneo.*;
import it.csi.jsf.web.pool.*;
import java.io.*;
import java.sql.*;
//import java.security.*;
import javax.sql.*;

/**
 * Title:        Agrichim - Front Office
 * Description:  Richiesta analisi chimiche su campioni biologici agrari
 * Copyright:    Copyright (c) 2003
 * Company:      CSI Piemonte - Progettazione e Sviluppo - Cuneo
 * @author Michele Piant�, Piergiorgio Chiriotti
 * @version 1.0.0
 */

/**
 * La classe Autenticazione viene utilizzata come classe base per memorizzare le
 * informazioni relative all'utente collegato al sistema (propriet� utente) e
 * gli eventuali problemi che possono verificarsi durante lesecuzione delle
 * query (query, contenutoQuery, errorCode)
 * E' un Java Bean che possiede le propriet�
 * -
 * - query: qui vengono memorizzate informazioni circa il metodo e la classe
 *   contenenti una query che ha generato l'errore
 * - contenutoQuery: viene memorizzata la query che ha causato un'errore
 * - errorCode: codice di errore
 * - dataSource
 * - utente
 * - queryRicerca: memorizza la query utilizzata nella ricerca delle'elenco
 *   delle analisi
 * - queryCountRicerca: come queryRicerca, solo che viene usata per contare
 *   il numero di record estratti
 * - idRichiestaCorrente: contiene l'id della richiesta attualmente in uso
 * - fase: la fase corrente della richiesta in bozza. inizialmente � 0
 * - coordinateGeografiche: se vale true significa che il materiale (TER, FOG,
 *   FRU) di cui si vuole fare l'analisi necessita dell'indicazione delle
 *   coordinate geografiche.
 * - codMateriale: codice materiale della richiesta in bozza corrente.
 *   Inizialmente � null.
 * - spedizioneFattura: se true significa che � stata richiesta la spedizione
 *   della fattura. Se false che non � stata richiesta
 * - costoAnalisi: memorizza al suo interno il costo totale dell'analisi
 *   di questo campione
 * - pa (booleano): default � false; se il login avviene da Rupar, allora TRUE
 */

public class AutenticazioneQry extends BeanDAO implements Serializable
{
  private static final long serialVersionUID = -3006332118842972235L;

  private String contenutoQuery;
  private String errorCode;
  private String query;
  private DataSource dataSource;
  private Utente utente;
  private String queryRicerca;
  private String queryCountRicerca;
  private long idRichiestaCorrente=0;
  private byte fase=0;
  private boolean coordinateGeografiche;
  private String codMateriale;
  private String istatComuneRichiesta;
  private String comuneRichiesta;
  private boolean spedizioneFattura;
  private double costoAnalisi;
  private Throwable eccezione;
  private boolean datiControllati=false;
  private boolean PA=false;
  //Elisa Poggio 20/10/2015 campo che 
  private boolean piemonte;

  public AutenticazioneQry()
  {
  }

  public void setContenutoQuery(String newContenutoQuery)
  {
    contenutoQuery = newContenutoQuery;
  }
  public String getContenutoQuery()
  {
    return contenutoQuery;
  }
  public void setErrorCode(String errorCode)
  {
    this.errorCode = errorCode;
  }
  public String getErrorCode()
  {
    return errorCode;
  }
  public void setQuery(String query)
  {
    this.query = query;
  }
  public String getQuery()
  {
    return query;
  }

  public void setDataSource(Object obj)
  {
    if (Utili.POOLMAN)
      this.setGenericPool((it.csi.jsf.web.pool.GenericPool)obj);
    else
      this.dataSource = (javax.sql.DataSource)obj;
  }

  public void setDataSource(javax.sql.DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public javax.sql.DataSource getDataSource()
  {
    return dataSource;
  }

  public Connection getConnection()
      throws Exception, SQLException
  {
    if (Utili.POOLMAN)
    {
      //Sono in ambiente TomCat quindi restuituisco una connessione fornita
      //da PoolMan
      return this.getGenericPool().getConnection();
    }
    else
    {
      //Sono in ambiente WebLogic quindi restuituisco una connessione fornita
      //da DataSource
      return dataSource.getConnection();
    }
  }

  public boolean isConnection()
      throws Exception, SQLException
  {
    if (Utili.POOLMAN)
    {
      //Sono in ambiente TomCat quindi controllo se Poolman � inizializzato
      if (this.getGenericPool() == null) return false;
      else return true;
    }
    else
    {
      //Sono in ambiente WebLogic quindi controllo se il DataSource �
      // inizializzato
      if (dataSource == null) return false;
      else return true;
    }
  }
  
  public void setUtente(Utente utente) {
    this.utente = utente;
  }
  public Utente getUtente() {
    return utente;
  }

  /**
   * Caricamento dei dati dell'utente necessari all'applicazione
   * @param identificativoUtenteIride viene passato da IRIDE SP
   * @param codiceFiscale viene passato da IRIDE SP
   * @param partitaIVA viene passato da IRIDE SP
   * @throws Exception DataSource non inizializzato
   * @throws SQLException errore generico SQL
   */
  public Utente caricaDati(String identificativoUtenteIride,
                         String codiceFiscale,
                         String partitaIVA)
     throws Exception, SQLException{
    utente = new Utente(identificativoUtenteIride,codiceFiscale,partitaIVA);
    CuneoLogger.debug(this,"sono in AutenticazioneQry.java / CaricaDati ");
    CuneoLogger.debug(this,"identificativoUtenteIride -> "+identificativoUtenteIride);
    CuneoLogger.debug(this,"codiceFiscale -> "+codiceFiscale);
    CuneoLogger.debug(this,"partitaIVA -> "+partitaIVA);
    if (!isConnection())
           throw new Exception("Riferimento a DataSource non inizializzato");
    Connection conn=this.getConnection();

    if (!utente.leggiAnagraficaUtente(conn)){
    	CuneoLogger.debug(this,"!utente.leggiAnagraficaUtente(conn) ");
      // Bisogna chiamare il modolo IRIDE SP per la restituzione di tutti
      // i dati necessari all'inserimento nella tabella Anagrafica
    }
    if (partitaIVA!=null)
      utente.leggiAnagraficaAzienda(conn);
    conn.close();
    CuneoLogger.debug(this,"utente -> "+utente.toString());
    return utente;
  }
  
  public void setQueryRicerca(String queryRicerca)
  {
    this.queryRicerca = queryRicerca;
  }
  public String getQueryRicerca()
  {
    return queryRicerca;
  }
  public void setQueryCountRicerca(String queryCountRicerca)
  {
    this.queryCountRicerca = queryCountRicerca;
  }
  public String getQueryCountRicerca()
  {
    return queryCountRicerca;
  }

  public void ricercaElencoCampioniBase()
  {
    this.ricercaElencoCampioniBase(false,null,null,null,null,
                                   null,null,null,null,null);
  }

  public void ricercaElencoCampioniBase(boolean filtri,
                                       String idRichiestaDa,
                                       String idRichiestaA,
                                       String dataDa,
                                       String dataA,
                                       String tipoMateriale,
                                       String codiceFiscale,
                                       String cognome,
                                       String nome,
                                       String etichetta)
  {
    String filtro="";
    boolean normale=true;
    if (filtri) filtro=impostaCriteriRicerca(idRichiestaDa,idRichiestaA,dataDa,
                                      dataA,tipoMateriale,codiceFiscale,
                                      cognome,nome,etichetta);

    if (filtro.length()>0 && filtro.charAt(0) == '1')
    {
      normale=false;
      filtro=filtro.substring(1);
    }

    /*
     Questa query mi informa sul numero di record restituiti per organizzare
     la visualizzazione della pagina
    */
    StringBuffer queryCount = new StringBuffer();
    if (normale)
     queryCount.append("SELECT COUNT(ID_RICHIESTA) AS NUM ");
    else
     queryCount.append("SELECT COUNT(DISTINCT ID_RICHIESTA) AS NUM ");
    queryCount.append("FROM ETICHETTA_CAMPIONE EC, MATERIALE M, ");
    if (normale)
     queryCount.append("CODIFICA_TRACCIABILITA CT ");
    else
     queryCount.append("CODIFICA_TRACCIABILITA CT, ANAGRAFICA A ");
    queryCount.append("WHERE EC.STATO_ATTUALE <> '00' ");
    if (!normale)
    {
      queryCount.append("AND (A.ID_ANAGRAFICA=EC.ANAGRAFICA_UTENTE OR ");
      queryCount.append("A.ID_ANAGRAFICA=EC.ANAGRAFICA_TECNICO OR ");
      queryCount.append("A.ID_ANAGRAFICA=EC.ANAGRAFICA_PROPRIETARIO) ");
    }
    queryCount.append("AND EC.CODICE_MATERIALE= M.CODICE_MATERIALE ");
    queryCount.append("AND EC.STATO_ATTUALE = CT.CODICE ");
    queryCount.append("AND (");
    queryCount.append("EC.ANAGRAFICA_UTENTE = ");
    queryCount.append(this.getUtente().getAnagraficaUtente());
    queryCount.append(" OR EC.ANAGRAFICA_TECNICO = ");
    queryCount.append(this.getUtente().getAnagraficaUtente());
    queryCount.append(" OR EC.ANAGRAFICA_PROPRIETARIO = ");
    queryCount.append(this.getUtente().getAnagraficaUtente());
    queryCount.append(" OR EC.ANAGRAFICA_PROPRIETARIO = ");
    queryCount.append(this.getUtente().getAnagraficaAzienda());
    queryCount.append(") ");
    queryCount.append(filtro);
    //CuneoLogger.debug(this,"queryCount "+queryCount.toString());
    /*
     Questa query invece mi restituisce i record
    */
    StringBuffer query = new StringBuffer("SELECT * FROM (");
    if (normale)
     query.append("SELECT EC.ID_RICHIESTA,");
    else
     query.append("SELECT DISTINCT EC.ID_RICHIESTA,");
    query.append("ROW_NUMBER() OVER (ORDER BY EC.ID_RICHIESTA DESC) AS NUM,");
    query.append("TO_CHAR(EC.DATA_INSERIMENTO_RICHIESTA,'DD/MM/YYYY') AS DATA,");
    query.append("EC.CODICE_MATERIALE AS CODICE_MATERIALE,");
    query.append("M.DESCRIZIONE AS MATERIALE,");
    query.append("EC.DESCRIZIONE_ETICHETTA,");
    query.append("CT.DESCRIZIONE AS DESC_STATO_ATTUALE,");
    query.append("EC.STATO_ATTUALE,");
    query.append("EC.ANAGRAFICA_UTENTE,");
    query.append("EC.ANAGRAFICA_TECNICO,");
    query.append("EC.ANAGRAFICA_PROPRIETARIO,");
    query.append("COALESCE(TABELLA_FATTURE.CONTEGGIO_FATTURE,0) AS CONTEGGIO_FATTURE,");
    query.append("TABELLA_FATTURE.NUMERO_FATTURA, ");
    query.append("TABELLA_FATTURE.ANNO AS ANNO_FATTURA ");
    query.append(" FROM ETICHETTA_CAMPIONE EC LEFT JOIN");
    query.append("(SELECT COUNT(CF.ID_RICHIESTA) CONTEGGIO_FATTURE, MAX(CF.NUMERO_FATTURA) NUMERO_FATTURA, MAX(CF.ANNO) ANNO, CF.ID_RICHIESTA ");
    query.append("FROM CAMPIONE_FATTURATO CF, FATTURA FA ");
    query.append("WHERE CF.ANNO = FA.ANNO ");
    query.append("AND CF.NUMERO_FATTURA = FA.NUMERO_FATTURA ");
    query.append("GROUP BY CF.ID_RICHIESTA) AS TABELLA_FATTURE USING (ID_RICHIESTA), ");
    query.append("MATERIALE M, ");
    if (normale)
    {
      query.append("CODIFICA_TRACCIABILITA CT ");
    }
    else
    {
      query.append("CODIFICA_TRACCIABILITA CT, ANAGRAFICA A ");
    }
    query.append("WHERE EC.STATO_ATTUALE <> '00' ");
    if (! normale)
    {
      query.append("AND (A.ID_ANAGRAFICA=EC.ANAGRAFICA_UTENTE OR ");
      query.append("A.ID_ANAGRAFICA=EC.ANAGRAFICA_TECNICO OR ");
      query.append("A.ID_ANAGRAFICA=EC.ANAGRAFICA_PROPRIETARIO) ");
    }
    query.append("AND EC.CODICE_MATERIALE= M.CODICE_MATERIALE ");
    query.append("AND EC.STATO_ATTUALE = CT.CODICE ");
    query.append("AND (");
    query.append("EC.ANAGRAFICA_UTENTE = ");
    query.append(this.getUtente().getAnagraficaUtente());
    query.append(" OR EC.ANAGRAFICA_TECNICO = ");
    query.append(this.getUtente().getAnagraficaUtente());
    query.append(" OR EC.ANAGRAFICA_PROPRIETARIO = ");
    query.append(this.getUtente().getAnagraficaUtente());
    query.append(" OR EC.ANAGRAFICA_PROPRIETARIO = ");
    query.append(this.getUtente().getAnagraficaAzienda());
    query.append(") ");
    query.append(filtro);
    query.append("ORDER BY EC.ID_RICHIESTA DESC");
    //CuneoLogger.debug(this,"query1 "+query.toString());
    this.setQueryCountRicerca(queryCount.toString());
    this.setQueryRicerca(query.toString());
  }

  public void ricercaElencoCampioniLaboratorio(String idRichiestaDa,
                                       String idRichiestaA,
                                       String dataDa,
                                       String dataA,
                                       String tipoMateriale,
                                       String codiceFiscale,
                                       String cognome,
                                       String nome,
                                       String etichetta)
  {
    boolean normale=true;
    String filtro=impostaCriteriRicerca(idRichiestaDa,idRichiestaA,dataDa,
                                      dataA,tipoMateriale,codiceFiscale,
                                      cognome,nome,etichetta);
    if (filtro.length()>0 && filtro.charAt(0) == '1')
    {
      normale=false;
      filtro=filtro.substring(1);
    }
    /*
      Questa query mi informa sul numero di record restituiti per organizzare
      la visualizzazione della pagina
    */

    StringBuffer queryCount = new StringBuffer();
    if (normale)
     queryCount.append("SELECT COUNT(ID_RICHIESTA) AS NUM ");
    else
     queryCount.append("SELECT COUNT(DISTINCT ID_RICHIESTA) AS NUM ");
    queryCount.append("FROM ETICHETTA_CAMPIONE EC, MATERIALE M, ");
    if (normale)
     queryCount.append("CODIFICA_TRACCIABILITA CT ");
    else
     queryCount.append("CODIFICA_TRACCIABILITA CT, ANAGRAFICA A ");
    queryCount.append("WHERE EC.STATO_ATTUALE <> '00' ");
    if (!normale)
    {
      queryCount.append("AND (A.ID_ANAGRAFICA=EC.ANAGRAFICA_UTENTE OR ");
      queryCount.append("A.ID_ANAGRAFICA=EC.ANAGRAFICA_TECNICO OR ");
      queryCount.append("A.ID_ANAGRAFICA=EC.ANAGRAFICA_PROPRIETARIO) ");
    }
    queryCount.append("AND EC.CODICE_MATERIALE= M.CODICE_MATERIALE ");
    queryCount.append("AND EC.STATO_ATTUALE = CT.CODICE ");
    queryCount.append(filtro);
    //CuneoLogger.debug(this,"queryCount "+queryCount.toString());

    /*
     Questa query invece mi restituisce i record
    */
    StringBuffer query = new StringBuffer("SELECT * FROM (");
    if (normale)
     query.append("SELECT EC.ID_RICHIESTA,");
    else
     query.append("SELECT DISTINCT EC.ID_RICHIESTA,");
    query.append("ROW_NUMBER() OVER (ORDER BY EC.ID_RICHIESTA DESC) AS NUM,");
    query.append("TO_CHAR(EC.DATA_INSERIMENTO_RICHIESTA,'DD/MM/YYYY')");
    query.append(" AS DATA,");
    query.append("EC.CODICE_MATERIALE AS CODICE_MATERIALE,");
    query.append("M.DESCRIZIONE AS MATERIALE,");
    query.append("EC.DESCRIZIONE_ETICHETTA,");
    query.append("CT.DESCRIZIONE AS DESC_STATO_ATTUALE,");
    query.append("EC.STATO_ATTUALE,");
    query.append("EC.ANAGRAFICA_UTENTE,");
    query.append("EC.ANAGRAFICA_TECNICO,");
    query.append("EC.ANAGRAFICA_PROPRIETARIO, ");
    query.append("COALESCE(TABELLA_FATTURE.CONTEGGIO_FATTURE,0) AS CONTEGGIO_FATTURE,");
    query.append("TABELLA_FATTURE.NUMERO_FATTURA, ");
    query.append("TABELLA_FATTURE.ANNO AS ANNO_FATTURA ");
    query.append(" FROM ETICHETTA_CAMPIONE EC LEFT JOIN");
    query.append("(SELECT COUNT(CF.ID_RICHIESTA) CONTEGGIO_FATTURE, MAX(CF.NUMERO_FATTURA) NUMERO_FATTURA, MAX(CF.ANNO) ANNO, CF.ID_RICHIESTA ");
    query.append("FROM CAMPIONE_FATTURATO CF, FATTURA FA ");
    query.append("WHERE CF.ANNO = FA.ANNO ");
    query.append("AND CF.NUMERO_FATTURA = FA.NUMERO_FATTURA ");
    query.append("GROUP BY CF.ID_RICHIESTA) AS TABELLA_FATTURE USING (ID_RICHIESTA), ");
    query.append("MATERIALE M, ");
    if (normale)
      query.append("CODIFICA_TRACCIABILITA CT ");
    else
      query.append("CODIFICA_TRACCIABILITA CT, ANAGRAFICA A ");
    query.append("WHERE EC.STATO_ATTUALE <> '00' ");
    if (!normale)
    {
      query.append("AND (A.ID_ANAGRAFICA=EC.ANAGRAFICA_UTENTE OR ");
      query.append("A.ID_ANAGRAFICA=EC.ANAGRAFICA_TECNICO OR ");
      query.append("A.ID_ANAGRAFICA=EC.ANAGRAFICA_PROPRIETARIO) ");
    }
    query.append("AND EC.CODICE_MATERIALE= M.CODICE_MATERIALE ");
    query.append("AND EC.STATO_ATTUALE = CT.CODICE ");
    query.append(filtro);
    query.append("ORDER BY EC.ID_RICHIESTA DESC");
    //CuneoLogger.debug(this,"query "+query.toString());
    this.setQueryCountRicerca(queryCount.toString());
    this.setQueryRicerca(query.toString());
  }

  public void ricercaElencoCampioniTecnico(String idRichiestaDa,
                                       String idRichiestaA,
                                       String dataDa,
                                       String dataA,
                                       String tipoMateriale,
                                       String codiceFiscale,
                                       String cognome,
                                       String nome,
                                       String etichetta)
  {
    String filtro=impostaCriteriRicerca(idRichiestaDa,idRichiestaA,dataDa,
                                      dataA,tipoMateriale,codiceFiscale,
                                      cognome,nome,etichetta);
    /*
      Questa query mi informa sul numero di record restituiti per organizzare
      la visualizzazione della pagina
    */
    if (filtro.length()>0 && filtro.charAt(0) == '1')
      filtro=filtro.substring(1);
    StringBuffer queryCount = new StringBuffer("SELECT COUNT(*) AS NUM ");
    queryCount.append("FROM ANAGRAFICA A,ETICHETTA_CAMPIONE EC, MATERIALE M, ");
    queryCount.append("CODIFICA_TRACCIABILITA CT ");
    queryCount.append("WHERE EC.STATO_ATTUALE <> '00' ");
    queryCount.append("AND EC.CODICE_MATERIALE= M.CODICE_MATERIALE ");
    queryCount.append("AND EC.STATO_ATTUALE = CT.CODICE ");
    queryCount.append("AND A.ID_ORGANIZZAZIONE = ");
    queryCount.append(this.getUtente().getIdOrganizzazione());
    queryCount.append(" AND (");
    queryCount.append("EC.ANAGRAFICA_UTENTE=A.ID_ANAGRAFICA ");
    queryCount.append("OR EC.ANAGRAFICA_TECNICO = A.ID_ANAGRAFICA");
    queryCount.append(") ");
    queryCount.append(filtro);
    //CuneoLogger.debug(this,"queryCount "+queryCount.toString());

    /*
     Questa query invece mi restituisce i record
    */
    StringBuffer query = new StringBuffer("SELECT * FROM (");
    query.append("SELECT ROW_NUMBER() OVER (ORDER BY EC.ID_RICHIESTA DESC) AS NUM,");
    query.append("EC.ID_RICHIESTA,");
    query.append("TO_CHAR(EC.DATA_INSERIMENTO_RICHIESTA,'DD/MM/YYYY')");
    query.append(" AS DATA,");
    query.append("EC.CODICE_MATERIALE AS CODICE_MATERIALE,");
    query.append("M.DESCRIZIONE AS MATERIALE,");
    query.append("EC.DESCRIZIONE_ETICHETTA,");
    query.append("CT.DESCRIZIONE AS DESC_STATO_ATTUALE,");
    query.append("EC.STATO_ATTUALE,");
    query.append("EC.ANAGRAFICA_UTENTE,");
    query.append("EC.ANAGRAFICA_TECNICO,");
    query.append("EC.ANAGRAFICA_PROPRIETARIO ");
    query.append("FROM ANAGRAFICA A,ETICHETTA_CAMPIONE EC, MATERIALE M, ");
    query.append("CODIFICA_TRACCIABILITA CT ");
    query.append("WHERE EC.STATO_ATTUALE <> '00' ");
    query.append("AND EC.CODICE_MATERIALE= M.CODICE_MATERIALE ");
    query.append("AND EC.STATO_ATTUALE = CT.CODICE ");
    query.append("AND A.ID_ORGANIZZAZIONE = ");
    query.append(this.getUtente().getIdOrganizzazione());
    query.append(" AND (");
    query.append("EC.ANAGRAFICA_UTENTE=A.ID_ANAGRAFICA ");
    query.append("OR EC.ANAGRAFICA_TECNICO = A.ID_ANAGRAFICA");
    query.append(") ");
    query.append(filtro);
    //CuneoLogger.debug(this,"query2 "+query.toString());
    this.setQueryCountRicerca(queryCount.toString());
    this.setQueryRicerca(query.toString());
  }

  private String impostaCriteriRicerca(String idRichiestaDa,
                                       String idRichiestaA,
                                       String dataDa,
                                       String dataA,
                                       String tipoMateriale,
                                       String codiceFiscale,
                                       String cognome,
                                       String nome,
                                       String etichetta)
  {
    StringBuffer filtro=new StringBuffer(" ");
    if (idRichiestaDa!=null)
      filtro.append(" AND EC.ID_RICHIESTA >= ").append(idRichiestaDa);
    if (idRichiestaA!=null)
      filtro.append(" AND EC.ID_RICHIESTA <= ").append(idRichiestaA);
    if (dataDa!=null)
    {
      filtro.append(" AND EC.DATA_INSERIMENTO_RICHIESTA >= to_timestamp('");
      filtro.append(dataDa).append("','DD/MM/YYYY') ");
    }
    if (dataA!=null)
    {
      filtro.append(" AND EC.DATA_INSERIMENTO_RICHIESTA <= to_timestamp('");
      filtro.append(dataA).append("','DD/MM/YYYY') ");
    }
    if (tipoMateriale!=null)
    {
      filtro.append(" AND EC.CODICE_MATERIALE = '").append(tipoMateriale);
      filtro.append("' ");
    }
    if (codiceFiscale!=null)
    {
      filtro.setCharAt(0,'1');
      filtro.append(" AND A.CODICE_IDENTIFICATIVO = '").append(codiceFiscale);
      filtro.append("' ");
    }
    if (cognome!=null)
    {
      filtro.setCharAt(0,'1');
      filtro.append(" AND UPPER(A.COGNOME_RAGIONE_SOCIALE) LIKE UPPER('%");
      filtro.append(Utili.toVarchar(cognome.trim())).append("%') ");
    }
    if (nome!=null)
    {
      filtro.setCharAt(0,'1');
      filtro.append(" AND UPPER(A.NOME) LIKE UPPER('%");
      filtro.append(Utili.toVarchar(nome.trim())).append("%') ");
    }
    if (etichetta!=null)
    {
      filtro.append(" AND UPPER(EC.DESCRIZIONE_ETICHETTA) LIKE UPPER('%");
      filtro.append(Utili.toVarchar(etichetta.trim())).append("%') ");
    }
    filtro.append(" ");
    return filtro.toString();
  }

  public void setIdRichiestaCorrente(long idRichiestaCorrente)
  {
    this.idRichiestaCorrente = idRichiestaCorrente;
  }

  public long getIdRichiestaCorrente()
  {
    return idRichiestaCorrente;
  }
  public void setFase(byte fase)
  {
    this.fase = fase;
  }
  public byte getFase()
  {
    return fase;
  }
  public void setCoordinateGeografiche(boolean coordinateGeografiche)
  {
    this.coordinateGeografiche = coordinateGeografiche;
  }
  public boolean isCoordinateGeografiche()
  {
    return coordinateGeografiche;
  }
  public void setCodMateriale(String codMateriale)
  {
    this.codMateriale = codMateriale;
  }
  public String getCodMateriale()
  {
    return codMateriale;
  }
  public void setSpedizioneFattura(boolean spedizioneFattura)
  {
    this.spedizioneFattura = spedizioneFattura;
  }
  public boolean isSpedizioneFattura()
  {
    return spedizioneFattura;
  }
  public void setCostoAnalisi(double costoAnalisi) {
    this.costoAnalisi = costoAnalisi;
  }
  public double getCostoAnalisi() {
    return costoAnalisi;
  }
  public String getComuneRichiesta()
  {
    return comuneRichiesta;
  }
  public void setComuneRichiesta(String comuneRichiesta)
  {
    this.comuneRichiesta = comuneRichiesta;
  }
  public String getIstatComuneRichiesta()
  {
    return istatComuneRichiesta;
  }
  public void setIstatComuneRichiesta(String istatComuneRichiesta)
  {
    this.istatComuneRichiesta = istatComuneRichiesta;
  }
  public Throwable getEccezione()
  {
    return eccezione;
  }
  public void setEccezione(Throwable eccezione)
  {
    this.eccezione = eccezione;
  }
  public boolean isDatiControllati()
  {
    return datiControllati;
  }
  public void setDatiControllati(boolean datiControllati)
  {
    this.datiControllati = datiControllati;
  }

  public void setPA(boolean PA)
  {
    this.PA = PA;
  }
  public boolean isPA()
  {
    return PA;
  }

	public boolean isPiemonte()
	{
		return piemonte;
	}

	public void setPiemonte(boolean piemonte)
	{
		this.piemonte = piemonte;
	}


}