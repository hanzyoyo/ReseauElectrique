package fournisseurs;
import java.util.ArrayList;

import client.ClientAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class FournisseurAgent extends Agent{

	private int prixaukilovente = 10;
	private int prixaukiloproduction;
	private int volumerestant;
	private int capital;
	private ArrayList<AID> clients = new ArrayList<AID>();
	private int demande=0;
	private int LT=3;  //Dur�e long terme
	private int CF=50000; //Cout Fixe de cr�er une installation
	private int capamoy=10; //capacit� moyenne d'une telle installation
	private int price_TIERS;
	private int nb_transport_perso=0;

	/*public FournisseurAgent(int prixvente,int prixprod, int volume,int capital){
		this.prixaukilovente=prixvente;
		this.prixaukiloproduction=prixprod;
		this.volumerestant=volume;
		this.capital=capital;
	}*/


	protected void setup() {

		// Printout a welcome message
		System.out.println("Le fournisseur "+getAID().getName()+" démarre sa production.");

		// Charger les comportements

		addBehaviour(new OneShotBehaviour(this) {
			//subscribe to DFService as producer of electricity
			@Override
			public void action() {
				// TODO Auto-generated method stub
				DFAgentDescription dfd = new DFAgentDescription();
				dfd.setName(getAID());
				ServiceDescription sd =	new	ServiceDescription();
				sd.setType("electricity-producer");
				sd.setName("production");
				dfd.addServices(sd);
				try{
					DFService.register(myAgent, dfd);
				}
				catch(FIPAException fe) {
					fe.printStackTrace();
				}
			}
		});

		//comportement gérant l'abonnement avec les clients
		addBehaviour(new CyclicBehaviour(this){
			private int step = 0;
			public void action() {
				FournisseurAgent myFournisseur = (FournisseurAgent)myAgent;
				ACLMessage msg;
				MessageTemplate mt;

				switch (step){
				case 0:
					mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
					msg=myAgent.receive(mt);
					if (msg!=null){							
						ACLMessage reply1=msg.createReply();
						reply1.setPerformative(ACLMessage.PROPOSE);
						reply1.setContent(String.valueOf(myFournisseur.prixaukilovente));
						myAgent.send(reply1);

						//log
						System.out.println("Producteur " + myAgent.getName() + " envoie proposition au Client " + (AID)reply1.getAllReceiver().next());

						step++;
					}
					else {
						block();
					} 
					break;
				case 1: 
					//need to make a template to limit search to proposal response
					mt = MessageTemplate.MatchConversationId("Subscription");
					msg=myAgent.receive(mt);
					if (msg!=null){
						if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL){

							//log
							System.out.println("Agent " + msg.getSender() + " refuse la proposition.");

							step+=-1;
							break;
						}
						else if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
							myFournisseur.clients.add(msg.getSender());
							ACLMessage reply2=msg.createReply();
							reply2.setPerformative(ACLMessage.INFORM);
							myAgent.send(reply2);

							//log
							System.out.println("Producteur " + myAgent.getName() + " confirme l'abonnement du Client " + msg.getAllReceiver());

							step = 0;
						}

					}
					break;
				}
			}
		});

		//ajout du comportement du fournisseur en fin de mois (facturation, éventuelle prise de décision de créer un réseau de transport...)
		addBehaviour(new MonthlyBehaviour());


	}
	
	public class MonthlyBehaviour extends CyclicBehaviour{
		private int Somme;
		private FournisseurAgent myFournisseur = (FournisseurAgent)myAgent;
		
		public void setSomme(int somme){
			this.Somme = somme;
		}
		
		public int getSomme(){
			return Somme;
		}
		
		public void action(){

			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchConversationId("top"));
			ACLMessage msg=myAgent.receive(mt);

			if (msg!=null){
				//log
				System.out.println("Producteur "+myAgent.getName()+" a reçu un top");
				Somme = 0;
				myAgent.addBehaviour(new FacturationClient(this));	
				
				//MaJ de la GUI
				myAgent.addBehaviour(new EnvoiGUI("Production mensuelle", Somme));				

				//on recalcule nos investissements tous les ans
				if (Integer.valueOf(msg.getContent())%12==0){
					myAgent.addBehaviour(new TransportCheckBehaviour(Somme,myFournisseur));
				}
			}
			else {
				block();
			}
		}
	}
	
	public class EnvoiGUI extends Behaviour{
		private String champ;
		private int valeur;
		
		public EnvoiGUI(String champ, int valeur){
			this.champ = champ;
			this.valeur = valeur;
		}

		@Override
		public void action() {
			//contacter le DFService pour obtenir l'AID de la GUI
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("electricity-producer");
			template.addServices(sd);
			try{
				DFAgentDescription[] results = DFService.search(myAgent, template);
				
				AID gui = results[0].getName();
				
				//envoi de la nouvelle production mensuelle à la GUI
				ACLMessage inf = new ACLMessage(ACLMessage.INFORM);
				inf.addReceiver(gui);
				//on se sert du conversationID pour passer le champ à MaJ
				inf.setConversationId(champ);
				inf.setContent(String.valueOf(valeur));
				
			}catch(FIPAException e){
				e.printStackTrace();
			}
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}

	public class FacturationClient extends Behaviour{

		private FournisseurAgent myFournisseur = (FournisseurAgent)myAgent;
		private Behaviour parentBehaviour;
		private int step = 0;
		private int somme = 0;
		
		public FacturationClient(Behaviour parentBehaviour){
			this.parentBehaviour = parentBehaviour;
		}
		
		
		@Override
		public void action() {
			switch(step){
			case 0:
				//demande des consommations aux clients
				ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
				for (int i=0;i<myFournisseur.clients.size();i++){
					req.addReceiver(myFournisseur.clients.get(i));
				}
				myFournisseur.send(req);

				//log
				System.out.println("Producteur "+myFournisseur.getName()+" a envoyé les demandes de consommation");

				step = 1;
				break;
			case 1:
				for (int i=0;i<myFournisseur.clients.size();i++){
					MessageTemplate mt1= MessageTemplate.and(MessageTemplate.MatchConversationId("conso"),MessageTemplate.MatchSender(myFournisseur.getClients().get(i)));
					ACLMessage msg1=myAgent.blockingReceive(mt1);
					this.somme+=Integer.valueOf(msg1.getContent());
				}
				step = 2;
				break;
			}
		}

		@Override
		public boolean done() {
			if(step == 2){
				//TODO : make parent behaviour not anonymous class to cast
				((MonthlyBehaviour)this.parentBehaviour).setSomme(this.somme);
				return true;
			}else
				return false;
		}

	}


	protected void produire1kilo(){
		this.volumerestant+=1;
		this.capital+=-1;
	}

	//	protected void essaivendre(ClientAgent c){
	//		if (c.veutELectricite && (c.getCapital()>this.prixaukilovente)){
	//			this.volumerestant+=-1;
	//			this.capital+=prixaukilovente;
	//			c.setCapital(c.getCapital()-this.prixaukilovente);}
	//		else {}
	//	}

	protected void takeDown() {
		//de-register service
		try{
			DFService.deregister(this);
		}
		catch(FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("Le fournisseur "+getAID().getName()+" ne vend plus d'electricit��.");
	}

	public int getLT() {
		return LT;
	}

	public void setLT(int lT) {
		LT = lT;
	}

	public int getCF() {
		return CF;
	}

	public void setCF(int cF) {
		CF = cF;
	}

	public int getCapamoy() {
		return capamoy;
	}

	public void setCapamoy(int capamoy) {
		this.capamoy = capamoy;
	}

	public int getPrice_TIERS() {
		return price_TIERS;
	}

	public void setPrice_TIERS(int price_TIERS) {
		this.price_TIERS = price_TIERS;
	}

	public int getNb_transport_perso() {
		return nb_transport_perso;
	}

	public void setNb_transport_perso(int nb_transport_perso) {
		this.nb_transport_perso = nb_transport_perso;
	}

	public ArrayList<AID> getClients() {
		return clients;
	}

	public void setClients(ArrayList<AID> clients) {
		this.clients = clients;
	}


}
