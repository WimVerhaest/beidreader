package be.senate.beidreader.controller;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
//import be.senate.belgium.eid.eidlib.BeID;
//import be.senate.belgium.eid.event.CardListener;
//import be.senate.belgium.eid.exceptions.EIDException;
import be.fedict.commons.eid.consumer.Address;
import be.fedict.commons.eid.consumer.BeIDIntegrity;
import be.fedict.commons.eid.consumer.CardData;
import be.fedict.commons.eid.consumer.Identity;
import be.senate.beidreader.model.CardHolder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

public class MainScreenController implements BeIDCardEventsListener {
    final private static short STATE_NEW = 0;
    final private static short STATE_OPENED = 1;
    final private static short STATE_NEWCHANGED = 2;
    final private static short STATE_OPENEDCHANGED = 3;

    private short state = STATE_NEW; // Initial state
    private Identity identity;
    private Address address;
    private CardHolder currentCardHolder;
    private HashMap<String, CardHolder> cardHolderHashMap;
    private ObservableList<CardHolder> cardHolderObservableList;
    private ObservableList<String> dummyList;
    private String defaultDirectory = "";

    @FXML private Stage mainStage;
    @FXML private GridPane rootGridPane;
    @FXML private TextField filePathTextField;
    @FXML private TextField rrnTextField;
    @FXML private TextField naamTextField;
    @FXML private TextField voornamenTextField;
    @FXML private ImageView pasfotoImageView;
    @FXML private ListView<CardHolder> cardHolderListView;
//    @FXML private ListView<String> cardHolderListView;
//    @FXML private Button upButton;
    @FXML private Button addButton;
    @FXML private Button deleteButton;

    @FXML private MenuItem newMenuItem;
    @FXML private MenuItem openMenuItem;
    @FXML private MenuItem saveMenuItem;
    @FXML private MenuItem saveAsMenuItem;
    @FXML private MenuItem closeMenuItem;
    @FXML private MenuItem exitMenuItem;




    // This method is automatically called by the 'load'-method of FMXLoader (see Main-class)
    public void initialize() {
        System.out.println("Initialize hoofdschermController");
        init();
    }

    // I have put most initialisation here...
    public void init() {
        this.cardHolderObservableList = FXCollections.observableArrayList();
        this.cardHolderListView.setItems(this.cardHolderObservableList);
        this.state = STATE_NEW;
        reflectState();
    }

    public void reflectState() {
        switch (this.state) {
            case (STATE_NEW) : {
                this.newMenuItem.setDisable(true);
                this.openMenuItem.setDisable(false);
                this.saveMenuItem.setDisable(true);
                this.saveAsMenuItem.setDisable(true);
                this.closeMenuItem.setDisable(true);
                this.exitMenuItem.setDisable(false);
                this.addButton.setDisable(true);
                this.deleteButton.setDisable(true);
                break;
            }
            case (STATE_OPENED) : {
                this.newMenuItem.setDisable(true);
                this.openMenuItem.setDisable(true);
                this.saveMenuItem.setDisable(true);
                this.saveAsMenuItem.setDisable(true);
                this.closeMenuItem.setDisable(false);
                this.exitMenuItem.setDisable(false);
                this.addButton.setDisable(true);
                this.deleteButton.setDisable(true);
                break;
            }
            case (STATE_NEWCHANGED) : {
                this.newMenuItem.setDisable(true);
                this.openMenuItem.setDisable(true);
                this.saveMenuItem.setDisable(false);
                this.saveAsMenuItem.setDisable(false);
                this.closeMenuItem.setDisable(false);
                this.exitMenuItem.setDisable(false);
                this.addButton.setDisable(true);
                this.deleteButton.setDisable(true);
                break;
            }
            case (STATE_OPENEDCHANGED) : {
                this.newMenuItem.setDisable(true);
                this.openMenuItem.setDisable(true);
                this.saveMenuItem.setDisable(false);
                this.saveAsMenuItem.setDisable(false);
                this.closeMenuItem.setDisable(false);
                this.exitMenuItem.setDisable(false);
                this.addButton.setDisable(true);
                this.deleteButton.setDisable(true);
                break;
            }
            default:;

        }
    }
    public void exitApplication(ActionEvent actionEvent) {
        System.exit(0);
        return;
    }

    public void setFilePath(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isDirectory()) {
                this.defaultDirectory = filePath;
            } else {
                this.defaultDirectory = file.getParent();
            }
            this.filePathTextField.setText(filePath);
        } else {
            this.filePathTextField.setText("");
        }
    }

    public void deleteButtonPushed(ActionEvent actionEvent) {
        this.currentCardHolder = this.cardHolderListView.getFocusModel().getFocusedItem();
        this.cardHolderObservableList.remove(this.currentCardHolder);
        refreshScreenDetail(this.currentCardHolder);
        this.deleteButton.setDisable(true);
        if (this.state == STATE_NEW)
            this.state = STATE_NEWCHANGED;
        else
            this.state = STATE_OPENEDCHANGED;
        reflectState();
    }

    // Methode that opens a filechooser, open the selected file en reads the content into the observable list of cardholders.
    public void startFileChooser(ActionEvent actionEvent) {
        System.out.println("Start fileChooser.");
        FileChooser fileChooser = new FileChooser();
        String currentFileName = filePathTextField.getText();
        File currentFile = new File(currentFileName);
        if (currentFile.exists()) {  // In which case the parent-directory also exists.
            File directory = currentFile.getParentFile();
            fileChooser.setInitialDirectory(directory);
        } else {
            fileChooser.setInitialDirectory(new File(this.defaultDirectory));
        }
        File file = fileChooser.showOpenDialog(mainStage);
        if (file == null) {
            filePathTextField.setText("");
        } else {
            String filename = file.getAbsolutePath();
            filePathTextField.setText(filename);
            setFilePath(filename);
            openFile(file);
        }
        return;
    }

    // Method that starts a file-save-chooser and saves (using saveToFile) the content of the observable list of cardholders to file.
    public void startFileSaveChooser(ActionEvent actionEvent) {
        System.out.println("Start fileSaveChooser.");
        FileChooser fileChooser = new FileChooser();
        String currentFileName = filePathTextField.getText();
        File currentFile = new File(currentFileName);
        if (currentFile.exists()) {
            File directory = currentFile.getParentFile();
            fileChooser.setInitialDirectory(directory);
        } else {
            fileChooser.setInitialDirectory(new File(this.defaultDirectory));
        }
//            fileChooser.setInitialFileName("");
        File file = fileChooser.showSaveDialog(mainStage);
        if (file == null) {
            filePathTextField.setText("");
        } else {
            saveToFile(file);
            String filename = file.getAbsolutePath();
            filePathTextField.setText(filename);
            setFilePath(filename);
        }
        return;
    }

    // Try to save on top of the current file
    public void saveCurrentFile(ActionEvent actionEvent) {
        String currentFileName = filePathTextField.getText();
        if ((currentFileName == null) || (currentFileName.equals(""))) { // No current file. We save using the fileChooser.
            startFileSaveChooser(actionEvent);
        } else {  // Here, we execute the 'NORMAL' action (i.e. we save using the current filename
            File file = new File(currentFileName);
            if (file == null) {
                filePathTextField.setText("");
            } else {
                String filename = file.getAbsolutePath();
                filePathTextField.setText(filename);
                saveToFile(file);
            }
        }
        return;
    }

    public void helpMenuItemClicked(ActionEvent actionEvent) {
        Stage helpSchermStage = new Stage();
        FXMLLoader helpSchermLoader = new FXMLLoader(getClass().getResource("helpScreen.fxml"));
        try {
            Pane helpSchermPane = helpSchermLoader.load();
            HelpScreenController helpScreenController = (HelpScreenController)helpSchermLoader.getController();
            Scene helpSchermScene = helpScreenController.getHelpWebScene();
            helpSchermStage.setScene(helpSchermScene);
            helpSchermStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void aboutMenuItemClicked(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("BeIDReader 1.2: (2017) wv@senate.be");
        alert.show();
    }

    // Methode that writes the observable list of cardholders to a given csv-file.
    private void saveToFile(File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
            PrintWriter printWriter = new PrintWriter(outputStreamWriter);
            Iterator<CardHolder> cardHolderIterator = this.cardHolderObservableList.iterator();
            while (cardHolderIterator.hasNext()) {
                CardHolder cardHolder = cardHolderIterator.next();
                printWriter.println(cardHolder.toCsv());
            };
            printWriter.close();
            this.state = STATE_OPENED;
            reflectState();
//            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    // Methode that fills the observable list of cardholders, given a csv-file with entries
    private void openFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            LineNumberReader lineNumberReader = new LineNumberReader(inputStreamReader);
            // So far, so good. We can reinitialize the observableList
            this.cardHolderObservableList.clear();
            String currentLine = null;
            do {
                currentLine = lineNumberReader.readLine();
                if (currentLine != null) {
                    CardHolder cardHolder = CardHolder.getInstanceFromCsv(currentLine);
                    this.cardHolderObservableList.add(cardHolder);
                }
            } while (currentLine != null);
            // Now, we set the correct 'state'
            this.state = STATE_OPENED;
            reflectState();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Methode that fills the observable list of cardholders, given the filename of a csv-file with entries
    public void openFile(String fileName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            LineNumberReader lineNumberReader = new LineNumberReader(inputStreamReader);
            // So far, so good. We can reinitialize the observableList
            this.cardHolderObservableList.clear();
            String currentLine = null;
            do {
                currentLine = lineNumberReader.readLine();
                if (currentLine != null) {
                    CardHolder cardHolder = CardHolder.getInstanceFromCsv(currentLine);
                    this.cardHolderObservableList.add(cardHolder);
                }
            } while (currentLine != null);
            // Now, we set the correct 'state'
            this.state = STATE_OPENED;
            reflectState();
        } catch (java.io.FileNotFoundException e) { // If the file does not exist, we make it "new"
            this.state = STATE_NEW;
            reflectState();
            this.setFilePath("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void addButtonPushed(ActionEvent actionEvent){
        System.out.println("Down button pushed");
        this.cardHolderObservableList.add(this.currentCardHolder);
        if (this.state == STATE_NEW)
            this.state = STATE_NEWCHANGED;
        else
            this.state = STATE_OPENEDCHANGED;
        reflectState();

        return;
    }


    public void cardHolderListViewItemClicked(MouseEvent mouseEvent) {
        this.currentCardHolder = this.cardHolderListView.getFocusModel().getFocusedItem();
        if (this.currentCardHolder != null) {
            refreshScreenDetail(this.currentCardHolder);
            this.deleteButton.setDisable(false);
        }
        System.out.println("ListView item clicked; " + this.cardHolderListView.getFocusModel().getFocusedItem());
    }

    public void closeMenuItemClicked(ActionEvent actionEvent) {
        this.currentCardHolder = null;
        this.rrnTextField.setText("");
        this.naamTextField.setText("");
        this.voornamenTextField.setText("");
        this.pasfotoImageView.setImage(null);
        this.addButton.setDisable(true);
        this.cardHolderObservableList = FXCollections.observableArrayList();
        this.cardHolderListView.setItems(this.cardHolderObservableList);
        this.state = STATE_NEW;
        reflectState();
        this.setFilePath("");
    }

    private void showCardHolderAsCurrent(CardHolder cardHolder) {
        rrnTextField.setText(cardHolder.getRegNr());
        naamTextField.setText(cardHolder.getLastName());
        voornamenTextField.setText(cardHolder.getFirstName());
    }


    public void setIdentity (Identity identity) {
        this.identity = identity;
    }

    public void setAddress (Address address) {
        this.address = address;
    }

    private void refreshScreenDetail(CardHolder cardHolder) {
        this.rrnTextField.setText(cardHolder.getRegNr());
        this.naamTextField.setText(cardHolder.getLastName());
        this.voornamenTextField.setText(cardHolder.getFirstName() + " " + cardHolder.getMiddleName());
        Image image = new Image(cardHolder.getPictureAsInputStream());
        this.pasfotoImageView.setImage(image);
    }


    @Override
    public void eIDCardEventsInitialized() {

    }

    @Override
    public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard beIDCard) {
        System.out.println("Kaart ingebracht.");
        this.currentCardHolder = new CardHolder();
        this.currentCardHolder.readBeID(beIDCard);
        refreshScreenDetail(this.currentCardHolder);
        this.addButton.setDisable(false);
    }

    @Override
    public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard beIDCard) {
        System.out.println("Kaart verwijderd.");
        this.currentCardHolder = null;
        this.rrnTextField.setText("");
        this.naamTextField.setText("");
        this.voornamenTextField.setText("");
        this.pasfotoImageView.setImage(null);
        this.addButton.setDisable(true);
    }

    // This (inner)-class is used to 'present' a CardHolder within the ListView as a string
    // It does not yet function correctly.
    public class CardHolderFormatCell extends ListCell<CardHolder> {
        public CardHolderFormatCell () {};
        @Override protected void updateItem(CardHolder item, boolean empty) {
            super.updateItem(item, empty);
            setText(item == null ? "" : item.getRegNr() + " " + item.getLastName() + " " + item.getFirstName());
        }
    }
}
