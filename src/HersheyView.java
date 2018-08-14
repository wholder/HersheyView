import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static javax.swing.JOptionPane.*;
import static javax.swing.JOptionPane.OK_OPTION;

class HersheyView extends JPanel {
  private List<HersheyGlyph>    glyphs = new ArrayList<>();
  private Map<Integer,Integer>  ascii = new HashMap<>();
  private Map<Integer,String>   family = new HashMap<>();
  private Map<String,List<Integer>>  families = new TreeMap<>();
  private Map<Integer,Integer>  hIndex = new HashMap<>();
  private int                   index;
  private boolean               showGrid, showLR, showOrigin;
  private double                zoom = 8;

  private HersheyView () {
    setPreferredSize(new Dimension(800, 800));
    // Build glyphs from James Hurt's ASCII format
    String font = getResource("hershey.txt");
    StringTokenizer tok = new StringTokenizer(font, "\n\r");
    List<String> lines = new ArrayList<>();
    // Step 1: recombine lines that were split at 72 character boundary
    while (tok.hasMoreElements()) {
      String line = tok.nextToken();
      boolean startOfGlyph = true;
      for (int ii = 0; ii < Math.min(4, line.length()); ii++) {
        char cc = line.charAt(ii);
        startOfGlyph &= (Character.isDigit(cc) || cc == ' ');
      }
      if (startOfGlyph) {
        lines.add(line);
      } else {
        int idx = lines.size() - 1;
        String p1 = lines.get(idx) + line;
        lines.remove(idx);
        lines.add(p1);
      }
    }
    // Step 2: Parse Hurt format and convert to Path2D.Double object
    int hdx = 0;
    for (String line : lines) {
      Path2D.Double path = new Path2D.Double();
      int code = Integer.parseInt(line.substring(0, 5).trim());
      int verts = Integer.parseInt(line.substring(5, 8).trim());
      int left = line.charAt(8) - 'R';
      int right = line.charAt(9) - 'R';
      boolean move = true;
      for (int ii = 0; ii < verts - 1; ii++) {
        int idx = ii * 2;
        char cx = line.charAt(10 + idx);
        char cy = line.charAt(11 + idx);
        if (cx == ' ' && cy == 'R') {
          move = true;
        } else {
          int xx = cx - 'R';
          int yy = cy - 'R';
          if (move) {
            path.moveTo(xx, yy);
            move = false;
          } else {
            path.lineTo(xx, yy);
          }
        }
      }
      hIndex.put(code, hdx++);
      glyphs.add(new HersheyGlyph(code, path, left, right));
    }
    // Build Hershey code to ASCII code lookup tables from ascii.txt file
    String lookup = getResource("ascii.txt");
    tok = new StringTokenizer(lookup, "\n\r");
    while (tok.hasMoreElements()) {
      String line = tok.nextToken();
      String[] parts = line.split(":");
      int code = 32;
      if (parts.length == 2) {
        List<Integer> hCodes = new ArrayList<>();
        String[] codes = parts[1].split(",");
        for (String tmp : codes) {
          String[] seq = tmp.split("-");
          if (seq.length == 1) {
            int val = Integer.parseInt(seq[0]);
            ascii.put(val, code++);
            hCodes.add(val);
            family.put(val, parts[0]);
          } else if (seq.length == 2) {
            int start = Integer.parseInt(seq[0]);
            int end = Integer.parseInt(seq[1]);
            for (int ii = start; ii <= end; ii++) {
              ascii.put(ii, code++);
              family.put(ii, parts[0]);
              hCodes.add(ii);
            }
          }
        }
        families.put(parts[0], hCodes);
      }
    }
  }

  private String getResource (String fileName) {
    InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
    if (is != null) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    }
    return "";
  }

  public void paint (Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHints(hints);
    AffineTransform af = new AffineTransform();
    Dimension dim = getSize();
    double xOff = dim.width / 2.0;
    double yOff = dim.height / 2.0;
    af.translate(xOff, yOff);
    af.scale(zoom, zoom);
    if (showGrid) {
      int jj = (int) (60 / (zoom / 8));
      // Draw Grid
      g2.setStroke(new BasicStroke((0.25f)));
      g2.setPaint(Color.blue);
      for (int ii = 0; ii < jj; ii++) {
        g2.draw(af.createTransformedShape(new Line2D.Double(ii, -jj, ii, +jj)));
        g2.draw(af.createTransformedShape(new Line2D.Double(-jj, ii, +jj, ii)));
        if (ii > 0) {
          g2.draw(af.createTransformedShape(new Line2D.Double(-ii, -jj, -ii, +jj)));
          g2.draw(af.createTransformedShape(new Line2D.Double(-jj, -ii, +jj, -ii)));
        }
      }
    }
    if (showOrigin) {
      // Draw cross at origin
      g2.setStroke(new BasicStroke((0.5f)));
      g2.setPaint(Color.red);
      g2.draw(af.createTransformedShape(new Line2D.Double(0, -1, 0, +1)));
      g2.draw(af.createTransformedShape(new Line2D.Double(-1, 0, +1, 0)));
    }
    HersheyGlyph item = glyphs.get(index);
    Rectangle bnds = item.path.getBounds();
    if (showLR) {
      // Draw left/right lines
      g2.setStroke(new BasicStroke((1.0f)));
      g2.setPaint(Color.red);
      g2.draw(af.createTransformedShape(new Line2D.Double(item.left, bnds.y, item.left, bnds.y + bnds.height)));
      g2.draw(af.createTransformedShape(new Line2D.Double(item.right, bnds.y, item.right, bnds.y + bnds.height)));
    }
    g2.setStroke(new BasicStroke((2.0f)));
    g2.setPaint(Color.black);
    g2.draw(af.createTransformedShape(item.path));
    g2.drawString("Code:   " + item.code + "  (0x" + Integer.toHexString(item.code).toUpperCase() + ")", 20, 20);
    if (ascii.containsKey(item.code)) {
      int asc = ascii.get(item.code);
      g2.drawString("ASCII:  " + asc + "  (0x" + Integer.toHexString(asc).toUpperCase() + ")", 20, 35);
      g2.drawString("Family: " + family.get(item.code), 20, 50);
    }
  }

  private Map<String,List<Integer>> getFamiles () {
    return families;
  }
  private int glyphCount () {
    return glyphs.size();
  }

  private void nextGlyph () {
    if (index < glyphs.size() - 1) {
      index++;
      repaint();
    }
  }

  private void prevGlyph () {
    if (index > 0) {
      index--;
      repaint();
    }
  }

  private void setGlyph (int index) {
    this.index = index;
    repaint();
  }

  private Path2D.Double getShape (int code) {
    return glyphs.get(hIndex.get(code)).path;
  }

  private void selectHersheyCode (int code) {
    this.index= hIndex.get(code);
    repaint();
  }

  private void showGrid (boolean enable) {
    showGrid = enable;
    repaint();
  }

  private void showLeftRight (boolean enable) {
    showLR = enable;
    repaint();
  }

  private void showOrigin (boolean enable) {
    showOrigin = enable;
    repaint();
  }

  private void setZoom (String zoom) {
    this.zoom = Double.parseDouble(zoom);
    repaint();
  }

  private Line2D.Double[] getVectors (int code) {
    return getVectors(glyphs.get(hIndex.get(code)));
  }

  private Line2D.Double[] getSelectedVectors () {
    return getVectors(glyphs.get(index));
  }

  private HersheyGlyph getGlyph (int code) {
    return glyphs.get(hIndex.get(code));
  }

  private  Line2D.Double[] getVectors (HersheyGlyph item) {
    ArrayList<Line2D.Double> lines = new ArrayList<>();
    PathIterator pi = item.path.getPathIterator(new AffineTransform());
    double xx = 0, yy = 0;
    double mX = 0, mY = 0;
    while (!pi.isDone()) {
      double[] coords = new double[6];
      int type = pi.currentSegment(coords);
      switch (type) {
      case PathIterator.SEG_CLOSE:
        lines.add(new Line2D.Double(xx, yy, mX, mY));
        break;
      case PathIterator.SEG_MOVETO:
        mX = xx = coords[0];
        mY = yy = coords[1];
        break;
      case PathIterator.SEG_LINETO:
        lines.add(new Line2D.Double(xx, yy, xx = coords[0], yy = coords[1]));
        break;
      default:
        System.out.println("Error, Unknown PathIterator Type: " + type);
        break;
      }
      pi.next();
    }
    return lines.toArray(new Line2D.Double[0]);
  }

  static class HersheyGlyph {
    private int           code, left, right;
    private Path2D.Double path;

    HersheyGlyph (int code, Path2D.Double path, int left, int right) {
      this.code = code;
      this.path = path;
      this.left = left;
      this.right = right;
    }
  }

  public static void main (String[] args) {
    JFrame frame = new JFrame("Hershey Font Viewer");
    Preferences  prefs = Preferences.userRoot().node(frame.getClass().getName());
    frame.setResizable(false);
    frame.setLayout(new BorderLayout());
    try {
      HersheyView hershey = new HersheyView();
      frame.add(hershey, BorderLayout.CENTER);
      JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, hershey.glyphCount() - 1, 0);
      slider.addChangeListener(ev -> hershey.setGlyph(slider.getValue()));
      JPanel bottomPane = new JPanel(new BorderLayout());
      bottomPane.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
      bottomPane.add(slider, BorderLayout.CENTER);
      // Define Left button
      JButton left = new JButton("\u25C0");
      left.addActionListener(e -> hershey.prevGlyph());
      left.setPreferredSize(new Dimension(24, 12));
      bottomPane.add(left, BorderLayout.WEST);
      // Define Right button
      JButton right = new JButton("\u25B6");
      right.addActionListener(e -> hershey.nextGlyph());
      right.setPreferredSize(new Dimension(24, 12));
      bottomPane.add(right, BorderLayout.EAST);
      // Add Control panel
      JPanel controls = new JPanel();
      controls.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
      JCheckBox origin = new JCheckBox("Show Origin");
      controls.add(origin);
      origin.addActionListener(ev -> hershey.showOrigin(origin.isSelected()));
      JCheckBox grid = new JCheckBox("Show Grid");
      controls.add(grid);
      grid.addActionListener(ev -> hershey.showGrid(grid.isSelected()));
      JCheckBox leftRight = new JCheckBox("Show L/R");
      controls.add(leftRight);
      leftRight.addActionListener(ev -> hershey.showLeftRight(leftRight.isSelected()));
      JComboBox<String> zoom = new JComboBox<>(new String[] {"8", "16", "32", "64"});
      zoom.setSelectedIndex(1);
      hershey.setZoom((String) zoom.getSelectedItem());
      zoom.addActionListener(ev -> hershey.setZoom((String) zoom.getSelectedItem()));
      controls.add(zoom);
      JButton vectors = new JButton("Show Vectors");
      controls.add(vectors);
      vectors.addActionListener(ev -> {
        JDialog dialog = new JDialog(frame, "Vectors", Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setLocationRelativeTo(hershey);
        JTextArea txt = new JTextArea();
        JScrollPane sPane = new JScrollPane(txt);
        txt.setMargin(new Insets(3, 3, 3, 3));
        txt.setEditable(true);
        txt.setFont(new Font("Courier", Font.PLAIN, 14));
        DecimalFormat df = new DecimalFormat("0");
        for (Line2D.Double line : hershey.getSelectedVectors()) {
          txt.append(pad(df.format(line.x1)));
          txt.append(", ");
          txt.append(pad(df.format(line.y1)));
          txt.append(", ");
          txt.append(pad(df.format(line.x2)));
          txt.append(", ");
          txt.append(pad(df.format(line.y2)));
          txt.append("\n");
        }
        txt.setCaretPosition(0);
        dialog.add(sPane, BorderLayout.CENTER);
        dialog.setSize(200, 300);
        Rectangle dim1 = frame.getBounds();
        Rectangle dim2 = vectors.getBounds();
        dialog.setLocation(dim1.x + dim2.x, dim1.y + dim2.y);
        dialog.setVisible(true);
      });
      // Add "Find Glyph" Menu Button
      JButton find = new JButton("Find Glyph");
      find.addActionListener(ae -> {
        JPopupMenu families = new JPopupMenu("");
        Map<String,List<Integer>> fMap = hershey.getFamiles();
        for (String familiy : fMap.keySet()) {
          List<Integer> hCodes = fMap.get(familiy);
          JMenu fMenu = new JMenu(familiy);
          families.add(fMenu);
          fMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected (MenuEvent es) {
              if ((ae.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                if (((JMenu)((JMenu) es.getSource()).getComponent()).getItemCount() == 0) {
                  JMenuItem mItem = new JMenuItem("Export Font Vectors");
                  fMenu.add(mItem);
                  mItem.addActionListener(ev2 -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Save Vector Data as Text File");
                    fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                    FileNameExtensionFilter nameFilter = new FileNameExtensionFilter("Vector Data (*.txt)", "txt");
                    fileChooser.addChoosableFileFilter(nameFilter);
                    fileChooser.setFileFilter(nameFilter);
                    fileChooser.setSelectedFile(new File(prefs.get("default.dir", "/")));
                    if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                      File sFile = fileChooser.getSelectedFile();
                      String fPath = sFile.getPath();
                      if (!fPath.contains(".")) {
                        sFile = new File(fPath + ".txt");
                      }
                      try {
                        if (!sFile.exists() ||
                          showConfirmDialog(frame, "Overwrite Existing file?", "Warning", YES_NO_OPTION, PLAIN_MESSAGE) == OK_OPTION) {
                          List<Integer> codes = hershey.families.get(familiy);

                          StringBuilder buf = new StringBuilder("// Font: " + familiy + " - ");
                          Rectangle rect = new Rectangle(0, 0, 0, 0);
                          for (int ii = 32; ii < 128; ii++) {
                            Line2D.Double[] vecs = hershey.getVectors(codes.get(ii - 32));
                            for (Line2D.Double line : vecs) {
                              rect.x = Math.min(rect.x, (int) line.x1);
                              rect.x = Math.min(rect.x, (int) line.x2);
                              rect.y = Math.min(rect.y, (int) line.y1);
                              rect.y = Math.min(rect.y, (int) line.y2);
                              rect.width = Math.max(rect.width, (int) line.x1);
                              rect.width = Math.max(rect.width, (int) line.x2);
                              rect.height = Math.max(rect.height, (int) line.y1);
                              rect.height = Math.max(rect.height, (int) line.y2);
                            }
                          }
                          buf.append("Bounds {");
                          buf.append(Integer.toString(rect.x));
                          buf.append(", ");
                          buf.append(Integer.toString(rect.y));
                          buf.append(", ");
                          buf.append(Integer.toString(rect.width));
                          buf.append(", ");
                          buf.append(Integer.toString(rect.height));
                          buf.append("} ");
                          buf.append(Integer.toString(rect.width - rect.x));
                          buf.append(" x ");
                          buf.append(Integer.toString(rect.height - rect.y));
                          buf.append(" - Note: {left,right},{x1,y1,x2,y2},..\n");

                          for (int ii = 32; ii < 128; ii++) {
                            buf.append("'");
                            buf.append((char) ii);
                            buf.append("':");
                            HersheyGlyph glyph =  hershey.getGlyph(codes.get(ii - 32));
                            Line2D.Double[] vecs = hershey.getVectors(glyph);
                            buf.append(Integer.toString(glyph.left));
                            buf.append(",");
                            buf.append(Integer.toString(glyph.right));
                            buf.append(vecs.length > 0 ? "|" : "");
                            for (int jj = 0; jj < vecs.length; jj++) {
                              Line2D.Double line = vecs[jj];
                              buf.append(Integer.toString((int) line.x1));
                              buf.append(",");
                              buf.append(Integer.toString((int) line.y1));
                              buf.append(",");
                              buf.append(Integer.toString((int) line.x2));
                              buf.append(",");
                              buf.append(Integer.toString((int) line.y2));
                              buf.append(jj < (vecs.length - 1) ? "|" : "");
                            }
                            buf.append("\n");
                          }
                          buf.append("\n");
                          FileOutputStream fileOut = new FileOutputStream(sFile);
                          fileOut.write(buf.toString().getBytes());
                          fileOut.close();
                          fileOut.close();
                        }
                      } catch (IOException ex) {
                        showMessageDialog(frame, "Unable to save file", "Error", PLAIN_MESSAGE);
                        ex.printStackTrace();
                      }
                      prefs.put("default.dir", sFile.getAbsolutePath());
                    }

                  });
                }
              } else {
                if (((JMenu)((JMenu) es.getSource()).getComponent()).getItemCount() == 0) {
                  JPopupMenu chars = fMenu.getPopupMenu();
                  chars.setLayout(new GridLayout(12, 8));
                  for (int ii = 32; ii < 128; ii++) {
                    int hCode = hCodes.get(ii - 32);
                    JMenuItem mItem = new JMenuItem(new Glyph(hershey.getShape(hCode)));
                    mItem.setIconTextGap(0);
                    mItem.addActionListener(ev -> hershey.selectHersheyCode(hCode));
                    Dimension dim = mItem.getPreferredSize();
                    mItem.setPreferredSize(new Dimension(dim.width - (dim.width / 4), dim.height));
                    chars.add(mItem);
                  }
                }
              }
            }

            @Override
            public void menuDeselected (MenuEvent e) {
            }

            @Override
            public void menuCanceled (MenuEvent e) {
            }
          });
        }
        families.show(find, find.getWidth() / 2, find.getHeight() / 2);
      });
      controls.add(find);
      frame.add(controls, BorderLayout.NORTH);
      frame.add(bottomPane, BorderLayout.SOUTH);
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setResizable(true);
      frame.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      showMessageDialog(null, ex.getMessage(), "Error", PLAIN_MESSAGE);
      System.exit(1);
    }
  }

  static class Glyph extends ImageIcon {
    private static Rectangle bounds = new Rectangle(24, 26);

    Glyph (Path2D.Double path) {
      BufferedImage bImg = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2 = bImg.createGraphics();
      RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2.setRenderingHints(hints);
      g2.setStroke(new BasicStroke((1.0f)));
      g2.setBackground(Color.white);
      g2.clearRect(0, 0, bounds.width, bounds.height);
      g2.setColor(Color.darkGray);
      AffineTransform at = AffineTransform.getTranslateInstance(bounds.width / 2.0, bounds.height / 2.0);
      g2.draw(at.createTransformedShape(path));
      g2.setColor(Color.white);
      setImage(bImg);
    }
  }


  private static String pad (String val) {
    StringBuilder valBuilder = new StringBuilder(val);
    while (valBuilder.length() < 3) {
      valBuilder.insert(0, " ");
    }
    val = valBuilder.toString();
    return val;
  }
}
