package gui;


import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;

import java.util.ArrayList;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import client.*;
import fournisseurs.*;

public class GUI extends Agent{
	private ZModel model = new ZModel();

	public void setup(){

		JFrame fenetre2 = new JFrame("Réseau Electrique");
		fenetre2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fenetre2.setPreferredSize(new Dimension(600, 400));

		JTable tableau = new JTable(this.model);

		fenetre2.add(new JScrollPane(tableau));

		fenetre2.pack();
		fenetre2.setVisible(true);
	}

	class ZModel extends AbstractTableModel{
		private String[][] data;
		private String[] title;

		//Constructeur
		public ZModel(){
			String  title[] = {"Fournisseur","Nombre de clients", "Production moyenne mensuelle", "Production totale"};
			String data[][] = {};
			this.title = title;
			this.data=data;
		}

		//Retourne le nombre de colonnes
		public int getColumnCount() {
			return this.title.length;
		}

		//Retourne le nombre de lignes
		public int getRowCount() {
			return this.data.length;
		}

		//Retourne la valeur à l'emplacement spécifié
		public Object getValueAt(int row, int col) {
			return this.data[row][col];
		}         

		//Retourne le titre de la colonne à l'indice spécifié
		public String getColumnName(int col) {
			return this.title[col];
		}
		
		public void setValue(String fournisseur,String champ,String valeur){
			
		}

	}
}
