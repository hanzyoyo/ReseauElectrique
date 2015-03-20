package fournisseurs;
import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class FournisseurAgent extends Agent{

	private int prixaukilovente = 15;
	private int prixaukiloproduction = 5;
	private int volumerestant;
	private double capital=100000;
	private ArrayList<AID> clients = new ArrayList<AID>();
	private int demande=0;
	private int LT=3;  //Dur�e long terme
	private int CF=1000; //Cout Fixe de cr�er une installation
	private int capamoy=10; //capacit� moyenne d'une telle installation
	private int price_TIERS=5;
	private int nb_transport_perso=0;
	private double production_totale=0;


	/*public FournisseurAgent(int prixvente,int prixprod, int volume,int capital){
		this.prixaukilovente=prixvente;
		this.prixaukiloproduction=prixprod;
		this.volumerestant=volume;
		this.capital=capital;
	}*/


	protected void setup() {

		// Printout a welcome message
		System.out.println("Le fournisseur "+getAID().getName()+" démarre sa production.");

		// Get the sell/buy prices of electricity in the arguments
		Object[] args = getArguments();

		if(args != null &&	args.length > 0) {
			prixaukilovente	= Integer.parseInt((String) args[0]);
			if (args.length > 1){
				prixaukiloproduction = Integer.parseInt((String) args[1]);
			}
		}

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
						reply1.setContent(String.valueOf(myFournisseur.prixaukilovente) + "/" + String.valueOf(myFournisseur.prixaukiloproduction));
						myAgent.send(reply1);

						//log
						System.out.println("Producteur " + myAgent.getLocalName() + " envoie proposition au Client " + ((AID)reply1.getAllReceiver().next()).getLocalName());

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
							System.out.println("Agent " + msg.getSender().getLocalName() + " refuse la proposition.");

							step+=-1;
							break;
						}
						else if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
							//ajout du client dans la base
							myFournisseur.clients.add(msg.getSender());
							
							//envoi de l'information à la GUI
							myAgent.addBehaviour(new EnvoiGUI("Nombre de clients", myFournisseur.clients.size()));
							
							//lui confirmer
							ACLMessage reply2=msg.createReply();
							reply2.setPerformative(ACLMessage.INFORM);
							myAgent.send(reply2);


							//log
							System.out.println("Producteur " + myAgent.getLocalName() + " confirme l'abonnement du Client " + ((AID)reply2.getAllReceiver().next()).getLocalName());

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
		private double Somme;
		private FournisseurAgent myFournisseur = (FournisseurAgent)myAgent;

		public void setSomme(double somme){
			this.Somme = somme;
		}

		public double getSomme(){
			return Somme;
		}

		public void action(){

			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchConversationId("top"));
			ACLMessage msg=myAgent.receive(mt);

			if (msg!=null){
				//log
				System.out.println("Producteur "+myAgent.getLocalName()+" a reçu un top");
				Somme = 0;
				boolean finAnnee = Integer.valueOf(msg.getContent())%12==0;
				myAgent.addBehaviour(new FacturationClient(this,finAnnee));	


			}
			else {
				block();
			}
		}
	}

	public class EnvoiGUI extends Behaviour{
		private String champ;
		private double valeur;
		private boolean foundGUI;

		public EnvoiGUI(String champ, double valeur){
			this.champ = champ;
			this.valeur = valeur;
		}

		@Override
		public void action() {
			//contacter le DFService pour obtenir l'AID de la GUI
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("gui");
			template.addServices(sd);
			try{
				DFAgentDescription[] results = DFService.search(myAgent, template);

				//on garde ce comportement tant que l'on obtient pas de GUI (ie elle n'est pas encore souscrite au DF)
				if(results.length != 0){
					foundGUI = true;
					AID gui = results[0].getName();

					//envoi de la nouvelle production mensuelle à la GUI
					ACLMessage inf = new ACLMessage(ACLMessage.INFORM);
					inf.addReceiver(gui);
					//on se sert du conversationID pour passer le champ à MaJ
					inf.setConversationId(champ);
					inf.setContent(String.valueOf(valeur));

					myAgent.send(inf);

					//log
					System.out.println("Producteur " + myAgent.getLocalName() + " a envoyé une nouvelle valeur pour " + champ);
				}
			}catch(FIPAException e){
				e.printStackTrace();
			}

		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return foundGUI;
		}

	}
	public class findprice_TIERS extends Behaviour{
		private boolean b=false;
		public void action() {
			//contacter le DFService pour obtenir le price_TIERS
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("electricity-transporter");
			template.addServices(sd);

			
			
			
			
			try{
				DFAgentDescription[] results = DFService.search(myAgent, template);

				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				msg.setContent("Demande Prix");
				msg.addReceiver(results[0].getName());
				myAgent.send(msg);
				MessageTemplate mt=MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchSender(results[0].getName()));

				/* step 1 j'�cris , step 2 je lis et block si rien, step 3 je traite (ou directement step 2)
				 * Pas OneShot mais Behaviour avec un done � la main */

				ACLMessage msgt=myAgent.receive(mt);
				if(msgt!=null){
					((FournisseurAgent) myAgent).setPrice_TIERS(Integer.valueOf(msgt.getContent()));
					b=true;
				}else{
					block();
				}

			}catch(FIPAException e){
				e.printStackTrace();
			}

		}
		public boolean done(){
			if (b){return true;}
			return false;
		}
	}
	public class FacturationClient extends Behaviour{

		private FournisseurAgent myFournisseur;
		private Behaviour parentBehaviour;
		private int step = 0;
		private double somme = 0;
		private boolean finAnnee;
		private boolean firsttime=true;

		public FacturationClient(Behaviour parentBehaviour, boolean finAnnee){
			this.parentBehaviour = parentBehaviour;
			this.finAnnee = finAnnee;
		}


		@Override
		public void action() {
			myFournisseur = (FournisseurAgent)myAgent;

			switch(step){
			case 0:
				//demande des consommations aux clients
				ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
				for (int i=0; i < myFournisseur.clients.size(); i++){
					req.addReceiver(myFournisseur.clients.get(i));
				}
				myFournisseur.send(req);

				//log
				System.out.println("Producteur "+myFournisseur.getLocalName()+" a envoy� les demandes de consommation");

				step = 1;
				break;
			case 1:
				for (int i = 0; i< myFournisseur.clients.size(); i++){
					MessageTemplate mt1= MessageTemplate.and(MessageTemplate.MatchConversationId("conso"),MessageTemplate.MatchSender(myFournisseur.getClients().get(i)));
					ACLMessage msg1=myAgent.blockingReceive(mt1);

					//log
					System.out.println("Producteur " + myAgent.getLocalName() + " a re�u consommation du client " + msg1.getSender().getLocalName());

					this.somme+=Double.valueOf(msg1.getContent());
					//Rajout d'une dynamique de flux d'argent sur le portefeuille (variable capital)
					myFournisseur.capital-=(this.somme)*myFournisseur.prixaukiloproduction; //payer la production
					if (firsttime){
					myAgent.addBehaviour(new findprice_TIERS());//le behaviour est execut� jusqu'� trouver le prix comme le prix ne change pas elle n'est execut�e qu'une fois
					firsttime=false;}
					
					myFournisseur.capital-=(this.somme-Math.min(myFournisseur.capamoy*myFournisseur.nb_transport_perso,this.somme))*myFournisseur.price_TIERS;//payer le transport
					System.out.println("Le capital est maintenant de : "+capital);
					myFournisseur.capital+=(this.somme)*myFournisseur.prixaukilovente; //Les clients qui ont r�pondu � la REQUEST ont pay�


			}
			step = 2;
			break;
		}

	}

		@Override
		public boolean done() {
			if(step == 2){
				//debug
				FournisseurAgent myFournisseur=(FournisseurAgent)myAgent;
				System.out.println("Facturation finie");
				double capital=myFournisseur.getCapital();
				
				//TODO : toujours nécessaire?
				((MonthlyBehaviour)parentBehaviour).setSomme(somme);
				myFournisseur.production_totale+=somme;
				//MaJ de la GUI
				myAgent.addBehaviour(new EnvoiGUI("Production mensuelle", somme));
				myAgent.addBehaviour(new EnvoiGUI("Production totale", myFournisseur.production_totale));
				myAgent.addBehaviour(new EnvoiGUI("Capital", capital));
				myAgent.addBehaviour(new EnvoiGUI("Nb transporteur", myFournisseur.nb_transport_perso));
				
				
				

				//on recalcule nos investissements tous les ans
				if(this.finAnnee){
						myAgent.addBehaviour(new TransportCheckBehaviour(somme,myFournisseur));
				}				
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

	public int getPrixaukilovente() {
		return prixaukilovente;
	}

	public void setPrixaukilovente(int prixaukilovente) {
		this.prixaukilovente = prixaukilovente;
	}

	public int getPrixaukiloproduction() {
		return prixaukiloproduction;
	}

	public void setPrixaukiloproduction(int prixaukiloproduction) {
		this.prixaukiloproduction = prixaukiloproduction;
	}

	public int getVolumerestant() {
		return volumerestant;
	}

	public void setVolumerestant(int volumerestant) {
		this.volumerestant = volumerestant;
	}

	public double getCapital() {
		return capital;
	}

	public void setCapital(double capital) {
		this.capital = capital;
	}

	public int getDemande() {
		return demande;
	}

	public void setDemande(int demande) {
		this.demande = demande;
	}

	


}
