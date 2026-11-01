package com.signbridge.ui;
import com.signbridge.ai.*;
import com.signbridge.camera.CameraService;
import com.signbridge.data.HistoryRepository;
import com.signbridge.model.Translation;
import com.signbridge.service.*;
import org.opencv.core.Mat;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class SignBridgeFrame extends JFrame {
  private final CameraService camera=new CameraService();
  private final DatasetService dataset=new DatasetService();
  private final KnnGestureModel classifier=new KnnGestureModel();
  private final SentenceBuilder sentence=new SentenceBuilder();
  private final SpeechService speech=new SpeechService();
  private final JLabel cameraView=new JLabel("Start camera to preview",SwingConstants.CENTER);
  private final JLabel status=new JLabel("● Ready — collect samples to train your model");
  private final JLabel prediction=new JLabel("UNKNOWN",SwingConstants.CENTER);
  private final JLabel confidence=new JLabel("No model prediction yet");
  private final JTextArea phrase=new JTextArea(3,25);
  private final JComboBox<String> labelBox=new JComboBox<>(GestureLabels.LABELS.toArray(String[]::new));
  private final JTextArea datasetInfo=new JTextArea();
  private final DefaultListModel<String> history=new DefaultListModel<>();
  private final JList<String> historyList=new JList<>(history);
  private final Color bg=new Color(14,21,36), panel=new Color(25,37,58), accent=new Color(103,86,230);
  private final javax.swing.Timer timer;
  private boolean predicting=false, captureBurst=false;
  private int burstRemaining=0, burstSaved=0;
  private long lastAccepted=0;
  private String lastLabel="";
  public SignBridgeFrame(){
    super("SignBridge-Java | Trainable ISL Gesture Prototype");
    setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1200,820); setMinimumSize(new Dimension(1000,700)); setLocationRelativeTo(null);
    getContentPane().setBackground(bg); setLayout(new BorderLayout(14,14)); ((JComponent)getContentPane()).setBorder(new EmptyBorder(16,16,16,16));
    add(header(),BorderLayout.NORTH); add(content(),BorderLayout.CENTER); add(footer(),BorderLayout.SOUTH);
    timer=new javax.swing.Timer(100,e->tick()); refreshDataset(); refreshHistory();
    try { if(classifier.load()) status.setText("● Existing trained model loaded"); } catch(Exception ignored){}
  }
  private JPanel header(){
    JPanel p=new JPanel(new BorderLayout()); p.setOpaque(false);
    JLabel title=new JLabel("  ◈  SignBridge-Java"); title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif",Font.BOLD,28));
    JLabel sub=new JLabel("  TRAINABLE STATIC GESTURE RECOGNITION • JAVA + OPENCV"); sub.setForeground(new Color(151,171,202)); sub.setFont(new Font("SansSerif",0,10));
    JPanel l=new JPanel(new GridLayout(2,1)); l.setOpaque(false); l.add(title); l.add(sub); status.setForeground(new Color(111,222,165)); status.setFont(new Font("SansSerif",1,11));
    p.add(l,BorderLayout.WEST); p.add(status,BorderLayout.EAST); return p;
  }
  private JPanel content(){
    JPanel p=new JPanel(new GridBagLayout()); p.setOpaque(false); GridBagConstraints c=new GridBagConstraints(); c.insets=new Insets(0,0,0,12); c.fill=GridBagConstraints.BOTH; c.weighty=1;
    c.gridx=0;c.weightx=.55;p.add(left(),c);c.gridx=1;c.weightx=.45;c.insets=new Insets(0,0,0,0);p.add(right(),c);return p;
  }
  private JPanel card(String name){
    JPanel p=new JPanel(new BorderLayout(8,9));p.setBackground(panel);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(47,63,90)),new EmptyBorder(13,13,13,13)));
    JLabel h=new JLabel(name);h.setForeground(new Color(224,233,247));h.setFont(new Font("SansSerif",1,13));p.add(h,BorderLayout.NORTH);return p;
  }
  private JComponent left(){
    JPanel p=new JPanel(new GridLayout(2,1,0,12));p.setOpaque(false);
    JPanel cam=card("LIVE CAMERA • CENTER YOUR HAND IN THE FRAME");
    cameraView.setOpaque(true);cameraView.setBackground(new Color(7,13,25));cameraView.setForeground(new Color(147,167,197));cameraView.setFont(new Font("SansSerif",0,15));
    cam.add(cameraView,BorderLayout.CENTER);
    JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));controls.setOpaque(false);
    JButton start=btn("Start Camera"),stop=btn("Stop"),predict=btn("Live Predict");
    start.addActionListener(e->{if(camera.start(0)){timer.start();status.setText("● Camera connected");}else status.setText("● Camera not found — check permissions");});
    stop.addActionListener(e->{timer.stop();camera.stop();cameraView.setIcon(null);cameraView.setText("Camera stopped");status.setText("● Camera stopped");});
    predict.addActionListener(e->{predicting=!predicting;predict.setText(predicting?"Stop Predict":"Live Predict");status.setText(predicting?"● Live prediction enabled":"● Prediction paused");});
    controls.add(start);controls.add(stop);controls.add(predict);cam.add(controls,BorderLayout.SOUTH);
    JPanel train=card("DATASET CAPTURE & MODEL TRAINING");
    JPanel form=new JPanel(new GridLayout(0,1,5,5));form.setOpaque(false);
    JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));row.setOpaque(false);labelBox.setBackground(new Color(37,51,76));labelBox.setForeground(Color.WHITE);
    row.add(new JLabel("Capture label:"));row.add(labelBox);JButton capture=btn("Capture 30");capture.addActionListener(e->beginCapture());row.add(capture);form.add(row);
    JButton trainBtn=btn("Train KNN Model");trainBtn.addActionListener(e->trainModel());form.add(trainBtn);
    datasetInfo.setEditable(false);datasetInfo.setBackground(new Color(12,19,32));datasetInfo.setForeground(new Color(191,207,229));datasetInfo.setFont(new Font("Monospaced",0,11));datasetInfo.setBorder(new EmptyBorder(8,8,8,8));
    form.add(new JScrollPane(datasetInfo));JLabel help=new JLabel("<html><font color='#AFC1DC'>Capture each sign in consistent lighting. Record 50–100+ samples per label and include OTHER.</font></html>");form.add(help);
    train.add(form,BorderLayout.CENTER);p.add(cam);p.add(train);return p;
  }
  private JComponent right(){
    JPanel p=new JPanel(new GridLayout(3,1,0,12));p.setOpaque(false);
    JPanel pred=card("MODEL PREDICTION");
    prediction.setFont(new Font("SansSerif",1,30));prediction.setForeground(new Color(136,226,178));pred.add(prediction,BorderLayout.CENTER);
    confidence.setForeground(new Color(172,190,216));confidence.setHorizontalAlignment(SwingConstants.CENTER);pred.add(confidence,BorderLayout.SOUTH);
    JPanel phraseCard=card("CONVERSATION BUILDER");
    phrase.setEditable(false);phrase.setLineWrap(true);phrase.setWrapStyleWord(true);phrase.setFont(new Font("SansSerif",1,19));phrase.setBackground(new Color(10,17,30));phrase.setForeground(Color.WHITE);phrase.setBorder(new EmptyBorder(10,10,10,10));
    phraseCard.add(new JScrollPane(phrase),BorderLayout.CENTER);
    JPanel actions=new JPanel(new GridLayout(2,3,6,6));actions.setOpaque(false);
    JButton add=btn("Add Prediction"),undo=btn("Undo"),clear=btn("Clear"),copy=btn("Copy"),speak=btn("Speak"),save=btn("Save Phrase");
    add.addActionListener(e->{String s=prediction.getText();if(!s.equals("UNKNOWN")&&!s.equals("OTHER")){sentence.add(s);updatePhrase();}});
    undo.addActionListener(e->{sentence.undo();updatePhrase();});clear.addActionListener(e->{sentence.clear();updatePhrase();});
    copy.addActionListener(e->{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(phrase.getText()),null);});
    speak.addActionListener(e->{try{speech.speak(phrase.getText());}catch(Exception ex){warn("Speech engine unavailable. On Linux install espeak.");}});
    save.addActionListener(e->savePhrase());
    for(JButton b:List.of(add,undo,clear,copy,speak,save))actions.add(b);phraseCard.add(actions,BorderLayout.SOUTH);
    JPanel hist=card("RECENT SAVED PHRASES");historyList.setBackground(panel);historyList.setForeground(new Color(210,222,240));hist.add(new JScrollPane(historyList),BorderLayout.CENTER);
    JButton refresh=btn("Refresh History");refresh.addActionListener(e->refreshHistory());hist.add(refresh,BorderLayout.SOUTH);
    p.add(pred);p.add(phraseCard);p.add(hist);return p;
  }
  private JPanel footer(){JPanel p=new JPanel(new BorderLayout());p.setOpaque(false);JLabel l=new JLabel("Prototype scope: static hand-pose classification only • Not a complete ISL sentence translator");l.setForeground(new Color(132,151,180));l.setFont(new Font("SansSerif",0,10));p.add(l,BorderLayout.WEST);return p;}
  private JButton btn(String s){JButton b=new JButton(s);b.setFocusPainted(false);b.setForeground(Color.WHITE);b.setBackground(accent);b.setFont(new Font("SansSerif",1,11));b.setBorder(new EmptyBorder(8,10,8,10));return b;}
  private void beginCapture(){
    if(!camera.isRunning()){warn("Start the camera before capturing samples.");return;}
    captureBurst=true;burstRemaining=30;burstSaved=0;status.setText("● Capturing samples — hold the selected pose");
  }
  private void tick(){
    Mat frame=camera.readMat();if(frame==null)return;
    BufferedImage image=camera.toBufferedImage(frame);
    if(image!=null){Image scaled=image.getScaledInstance(Math.max(1,cameraView.getWidth()),Math.max(1,cameraView.getHeight()),Image.SCALE_FAST);cameraView.setText("");cameraView.setIcon(new ImageIcon(scaled));}
    if(captureBurst){
      try{dataset.saveBurstFrame((String)labelBox.getSelectedItem(),frame);burstSaved++;burstRemaining--;
        if(burstRemaining<=0){captureBurst=false;status.setText("● Captured "+burstSaved+" samples for "+labelBox.getSelectedItem());refreshDataset();}
      }catch(IOException ex){captureBurst=false;warn(ex.getMessage());}
    }
    if(predicting){
      try{
        KnnGestureModel.Prediction p=classifier.predict(frame);String label=p.label();
        prediction.setText(GestureLabels.display(label));confidence.setText(String.format(Locale.ROOT,"Similarity indicator: %.1f (not calibrated probability)",p.similarity()));
        // Require repeated same prediction and a short cooldown before auto-adding.
        long now=System.currentTimeMillis();
        if(!label.equals("OTHER")&&label.equals(lastLabel)&&p.similarity()>65&&now-lastAccepted>1800){sentence.add(label);updatePhrase();lastAccepted=now;}
        lastLabel=label;
      }catch(Exception ex){prediction.setText("UNKNOWN");confidence.setText("Train a model to enable predictions");}
    }
    frame.release();
  }
  private void trainModel(){
    status.setText("● Training model...");
    new SwingWorker<Integer,Void>(){
      protected Integer doInBackground() throws Exception{return classifier.train(dataset.root());}
      protected void done(){try{int n=get();status.setText("● Model trained with "+n+" images");refreshDataset();}catch(Exception ex){status.setText("● Training failed");warn(ex.getCause()==null?ex.getMessage():ex.getCause().getMessage());}}
    }.execute();
  }
  private void refreshDataset(){
    try{StringBuilder b=new StringBuilder("LABEL                 SAMPLES\\n--------------------------------\\n");
      for(var e:dataset.counts().entrySet())b.append(String.format("%-20s %5d%n",e.getKey(),e.getValue()));
      datasetInfo.setText(b.toString());}catch(IOException e){datasetInfo.setText("Dataset unavailable: "+e.getMessage());}
  }
  private void updatePhrase(){phrase.setText(sentence.build());}
  private void savePhrase(){
    if(sentence.build().isBlank()){warn("Add a prediction to the conversation first.");return;}
    try{HistoryRepository.save(sentence.build());refreshHistory();status.setText("● Phrase saved to SQLite");}
    catch(SQLException e){warn(e.getMessage());}
  }
  private void refreshHistory(){
    try{history.clear();DateTimeFormatter f=DateTimeFormatter.ofPattern("dd MMM HH:mm");
      for(Translation t:HistoryRepository.latest(10))history.addElement(t.createdAt().format(f)+"  •  "+t.text());}
    catch(SQLException e){warn("History error: "+e.getMessage());}
  }
  private void warn(String m){JOptionPane.showMessageDialog(this,m,"SignBridge",JOptionPane.WARNING_MESSAGE);}
  @Override public void dispose(){timer.stop();camera.stop();super.dispose();}
}
