package fournisseurs;
import java.util.ArrayList;

import client.ClientAgent;
import client.ClientAgent.SubscriptionBehaviour;
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
	private int LT=3;  //Durée long terme
	private int CF=50000; //Cout Fixe de créer une installation
	private int capamoy=10; //capacité moyenne d'une telle installation
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
		System.out.println("Le fournisseur "+getAID().getName()+" dÃ©marre sa production.");

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

		/*		addBehaviour(new CyclicBehaviour(this) {

			@Override
			public void action() {
				if ((demande>0) && (capital>prixaukiloproduction)){
					((FournisseurAgent) myAgent).produire1kilo();
					for(int i=0 ; i < ((FournisseurAgent) myAgent).clients.size() ; i++){
						//((FournisseurAgent) myAgent).essaivendre(((FournisseurAgent) myAgent).clients.get(i));
					}
				}
				else if (capital<prixaukiloproduction){
					System.out.println("Le fournisseur "+getAID().getName()+" fait faillite.");
					doDelete();
				}
				else{}				
			}
		});*/

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

		addBehaviour(new CyclicBehaviour(this){
			public void action(){
				FournisseurAgent myFournisseur = (FournisseurAgent)myAgent;

				MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchConversationId("top"));
				ACLMessage msg=myAgent.receive(mt);

				if (msg!=null){
					int Somme=0;
					for (int i=0;i<myFournisseur.clients.size();i++){
						MessageTemplate mt1= MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchSender(myFournisseur.getClients().get(i)));
						ACLMessage msg1=myAgent.receive(mt1);
						Somme+=Integer.valueOf(msg1.getContent());
					}
					System.out.println("Producteur "+myAgent.getName()+" a reÃ§u un top");

					if (Integer.valueOf(msg.getContent())%12==0){
						myAgent.addBehaviour(new OneShotBehaviour(this){
							public void action(){

								double conso_stat=Somme/12;
								
								Somme=0;

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
									
									ACLMessage msgt=myAgent.receive(mt);
									if(msg!=null){
										 ((FournisseurAgent) myAgent).setPrice_TIERS(Integer.valueOf(msgt.getContent()));
									}else{
										block();
									}
											
								}catch(FIPAException e){
									e.printStackTrace();
								}

								double deltat=(CF/(Math.min(myFournisseur.getCapamoy(),conso_stat)*(myFournisseur.getPrice_TIERS())));
								if (LT>deltat){myFournisseur.setNb_transport_perso(myFournisseur.getNb_transport_perso()+1);}
								else {}



							}	

						});
					}
					ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
					for (int i=0;i<myFournisseur.clients.size();i++){
						req.addReceiver(myFournisseur.clients.get(i));
					}
					myFournisseur.send(req);

					//log
					System.out.println("Producteur "+myFournisseur.getName()+" a envoyÃ© les demandes de consommation");
				}
				else {
					block();
				}
			}
		}}}
});




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
			System.out.println("Le fournisseur "+getAID().getName()+" ne vend plus d'electricitï¿½ï¿½.");
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
