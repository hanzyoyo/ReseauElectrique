package gui;


import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import client.*;
import fournisseurs.*;

public class GUI extends Agent{
	private ZModel model = new ZModel();
	private Hashtable<AID, DataProducer> table = new Hashtable<>();
	private JTable tableau;
	private JFrame fenetre2;

	public Hashtable<AID, DataProducer> getTable() {
		return table;
	}

	public void setTable(Hashtable<AID, DataProducer> table) {
		this.table = table;
	}

	public void setup(){
		
		//comportement qui inscrit l'agent gui au DFService
		addBehaviour(new OneShotBehaviour(this) {
			//subscribe to DFService as producer of electricity
			@Override
			public void action() {
				// TODO Auto-generated method stub
				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(getAID());
				ServiceDescription sd =	new	ServiceDescription();
				sd.setType("gui");
				sd.setName("gui");
				dfd.addServices(sd);
				try{
					DFService.register(myAgent, dfd);
					
					//log
					System.out.println("GUI enregistrée auprès du DF");
				}
				catch(FIPAException fe) {
					fe.printStackTrace();
				}
			}
		});
		
		//comportement qui traite le flux d'informations des producteurs pour MaJ la GUI
		addBehaviour(new CyclicBehaviour(this) {
			
			@Override
			public void action() {
				// TODO un message pour tous les champs?
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				ACLMessage msg = myAgent.receive(mt);
				if(msg != null){					
					AID fournisseur = msg.getSender();
					String champ = msg.getConversationId();
					String valeur = msg.getContent();
					
					
					
					//create new line in hastable if non-existent
					if(!table.containsKey(fournisseur)){
						table.put(fournisseur, new DataProducer());
					}
					
					//change the value of the field
					//TODO: constantes globales pour éviter les divergences en cas de MaJ
					if(champ.equals("Nombre de clients")){
						table.get(fournisseur).setNbClients((int)Double.parseDouble(valeur));
					}else if(champ.equals("Production mensuelle")){
						table.get(fournisseur).setProdMensuelle(Math.round(Double.parseDouble(valeur)));
					}else if(champ.equals("Production totale")){
						table.get(fournisseur).setProdTotale(Math.round(Double.parseDouble(valeur)));						
					}else if(champ.equals("Capital")){
						table.get(fournisseur).setCapital(Math.round(Double.parseDouble(valeur)));
					}else if(champ.equals("Nb transporteur")){
						table.get(fournisseur).setNbtransport((int)Double.parseDouble(valeur));
					}
					
					tableau.revalidate();
					fenetre2.repaint();
				}else{
					block();
				}
			}
		});

		fenetre2 = new JFrame("Réseau Electrique");
		fenetre2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fenetre2.setPreferredSize(new Dimension(600, 400));

		tableau = new JTable(this.model);

		fenetre2.add(new JScrollPane(tableau));

		fenetre2.pack();
		fenetre2.setVisible(true);
	}


	class DataProducer{
		private int nbClients = 0;
		private double prodMensuelle = 0;
		private double prodTotale = 0;
		private double capital=0;
		private double nbtransport=0;
		
		public double getCapital() {
			return this.capital;
		}
		public void setCapital(double capital) {
			this.capital = capital;
		}
		public double getNbtransport() {
			return this.nbtransport;
		}
		public void setNbtransport(double transp) {
			this.nbtransport=transp;
		}


		public int getNbClients() {
			return nbClients;
		}
		public void setNbClients(int nbClients) {
			this.nbClients = nbClients;
		}
		public double getProdMensuelle() {
			return prodMensuelle;
		}
		public void setProdMensuelle(double prodMensuelle) {
			this.prodMensuelle = prodMensuelle;
		}
		public double getProdTotale() {
			return prodTotale;
		}
		public void setProdTotale(double prodTotale) {
			this.prodTotale = prodTotale;
		}
	}

	class ZModel extends AbstractTableModel{
		private String[] title;

		//Constructeur
		public ZModel(){
			String  title[] = {"Fournisseur","Nombre de clients", "Production mensuelle", "Production totale","Capital","Nb transporteur"};
			this.title = title; 
		}

		//Retourne le nombre de colonnes
		public int getColumnCount() {
			return this.title.length;
		}

		//Retourne le nombre de lignes
		public int getRowCount() {
			return table.size();
		}

		//Retourne la valeur à l'emplacement spécifié
		public Object getValueAt(int row, int col) {
			if (col == 0) {
				return getKey(row).getLocalName();
			} else if (col == 1){
				return table.get(getKey(row)).getNbClients();
			} else if (col == 2){
				return table.get(getKey(row)).getProdMensuelle();
			}else if (col == 3){
				return table.get(getKey(row)).getProdTotale();
			}else if (col ==4){
				return table.get(getKey(row)).getCapital();
			}else {
				return table.get(getKey(row)).getNbtransport();
			}
		}         

		private AID getKey(int a_index) {
			AID retval = null;
			Enumeration<AID> e = table.keys();
			for (int i = 0; i < a_index + 1; i++) {
				retval = e.nextElement();
			} // for

			return retval;
		}

		//Retourne le titre de la colonne à l'indice spécifié
		public String getColumnName(int col) {
			return this.title[col];
		}

	}
}
