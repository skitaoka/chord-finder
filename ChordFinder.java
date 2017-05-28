import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

final class ChordFinder extends JPanel {

  private static enum ChordType {
    em    (1 | (1<<3) | (1<<7)),
    e     (1 | (1<<4) | (1<<7)),
    esus4 (1 | (1<<5) | (1<<7)),
    eaug  (1 | (1<<4) | (1<<8)),
    edim  (1 | (1<<3) | (1<<6)),
    eadd9 (1 | (1<<4) | (1<<7) | (1<<2)),
    eM7   (1 | (1<<4) | (1<<7) | (1<<11)),
    em7   (1 | (1<<3) | (1<<7) | (1<<10)),
    e7    (1 | (1<<4) | (1<<7) | (1<<10)),
    e7sus4(1 | (1<<5) | (1<<7) | (1<<10)),
    em7b5 (1 | (1<<3) | (1<<6) | (1<<10)),
    emM7  (1 | (1<<3) | (1<<7) | (1<<11)),
    edim7 (1 | (1<<3) | (1<<6) | (1<< 9));

    private final int bits;

    private ChordType(final int type) {
      this.bits = type;
    }

    public final int bits() {
      return this.bits;
    }
  };

  private final String[][] keyNames = new String[][] {
      {"C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"},
      {"Db", "Ebb(=D)", "Eb", "Fb(=E)", "F", "Gb", "G", "Ab", "Bbb(=A)", "Bb", "Cb(=B)", "C"},
      {"D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "C", "C#"},
      {"Eb", "Fb=(E)", "F", "Gb", "G", "Ab", "A", "Bb", "Cb(=B)", "C", "Db", "D"},
      {"E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#"},
      {"F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E"},
      {"F#", "G", "G#", "A", "A#", "B", "B#(=C)", "C#", "D", "D#", "E", "E#(=F)"},
      {"G", "Ab", "A", "Bb", "B", "C", "C#", "D", "Eb", "E", "F", "F#"},
      {"Ab", "Bbb(=A)", "Bb", "Cb(=B)", "C", "Db", "D", "Eb", "Fb(=E)", "F", "Gb", "G"},
      {"A", "Bb", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"},
      {"Bb", "Cb(=B)", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A"},
      {"B", "C", "C#", "D", "D#", "E", "E#(=F)", "F#", "G", "G#", "A", "A#"},
    };
  private final JRadioButton[]        rbKey       = new JRadioButton[keyNames.length];
  private final JComboBox<ScaleType>  cbScales    = new JComboBox<>(ScaleType.values());
  private final JToggleButton[]       tbScale     = new JToggleButton[rbKey.length];
  private final JTextArea             taResults   = new JTextArea();
  private final JToggleButton[]       tbCharacter = new JToggleButton[rbKey.length];

  public ChordFinder() {
    super(new BorderLayout());

    final ActionListener action = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent evt) {
        final StringBuilder sb = new StringBuilder();

        // スケールを特性音の選択に反映する。
        applyScaleToCharacterTone();

        // スケールを列挙する
        final int scaleBits = getScaleBits();
        for (final ScaleType st : ScaleType.values()) {
          if (scaleBits == st.bits()) {
            sb.append(st.name());
            sb.append(' ');
          }
        }
        if (sb.length() == 0) {
          sb.append("[Unknown]");
        }
        sb.append('\n');
        sb.append('\n');

        // 特性音をビット表現で得る。
        final int characterBits = scaleBits & getCharacterBits();

        // スケールにマッチするコードを列挙する。
        final int keyIndex = getTonicTone(); // 主音
        for (int i = 0, size = rbKey.length; i < size; ++i) {
          final StringBuilder tmp = new StringBuilder();
          boolean hasCord = false;
          for (final ChordType ct : ChordType.values()) {
            final int type = ct.bits();
            final int chordBits = ((type << i) & 0xFFF) | (type >> (12 - i));
            if (((chordBits & scaleBits) == chordBits)
             && ((chordBits & characterBits) == characterBits))
            {
              hasCord = true;
              tmp.append(keyNames[keyIndex][i]);
              tmp.append(ct.name().substring(1));
              tmp.append(' ');
            }
          }
          if (hasCord) {
            switch (i % 3) {
            case 0: sb.append("T: "); break;
            case 1: sb.append("D: "); break;
            case 2: sb.append("S: "); break;
            }
            sb.append(tmp.toString());
            sb.append('\n');
          }
        }
        taResults.setText(sb.toString());
      }
    };
    {
      final JPanel pnlNorth = new JPanel();
      this.add(pnlNorth, BorderLayout.NORTH);
      {
        final ButtonGroup g = new ButtonGroup();
        for (int i = 0, size = keyNames.length; i < size; ++i) {
          pnlNorth.add(rbKey[i] = new JRadioButton(keyNames[i][0]));
          rbKey[i].addActionListener(action);
          g.add(rbKey[i]);
        }
        rbKey[0].setSelected(true);
      }
      {
        pnlNorth.add(cbScales);
        cbScales.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(final ActionEvent evt) {
            final ScaleType st = (ScaleType) cbScales.getSelectedItem();
            final int scaleBits = st.bits();
            for (int i = 0, size = keyNames.length; i < size; ++i) {
              tbScale[i].setSelected(((scaleBits >> i) & 1) == 1);
            }
            action.actionPerformed(evt);
          }
        });
      }
      // prev
      {
        final JButton btnPrev = new JButton("<");
        pnlNorth.add(btnPrev);

        btnPrev.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(final ActionEvent evt) {
            final int index = cbScales.getSelectedIndex();
            final int count = cbScales.getItemCount();
            cbScales.setSelectedIndex((index + count - 1) % count);
          }
        });
      }
      // next
      {
        final JButton btnNext = new JButton(">");
        pnlNorth.add(btnNext);

        btnNext.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(final ActionEvent evt) {
            final int index = cbScales.getSelectedIndex();
            final int count = cbScales.getItemCount();
            cbScales.setSelectedIndex((index + 1) % count);
          }
        });
      }
    }
    {
      final JPanel pnlSouth = new JPanel();
      this.add(pnlSouth, BorderLayout.SOUTH);
      {
        final String[] scaleNames = new String[] {"1", "♭2", "2", "♭3", "3", "4", "♯4", "5", "♭6", "6", "♭7", "7"};
        for (int i = 0, size = tbScale.length; i < size; ++i) {
          pnlSouth.add(tbScale[i] = new JToggleButton(scaleNames[i]));
          tbScale[i].addActionListener(action);
        }
        tbScale[0].setEnabled(false);
      }
      {
        // コード進行(Chord Progression)
        final JTextField tfCP = new JTextField(26);
        pnlSouth.add(tfCP);

        tfCP.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(final ActionEvent evt) {
            taResults.setText(parseChords(tfCP.getText()));
          }
        });
      }
    }
    {
      final JPanel pnlEast = new JPanel(new GridLayout(tbCharacter.length, 1));
      this.add(pnlEast, BorderLayout.EAST);
      {
        final String[] scaleNames = new String[] {"1", "♭2", "2", "♭3", "3", "4", "♯4", "5", "♭6", "6", "♭7", "7"};
        for (int i = 0, size = tbCharacter.length; i < size; ++i) {
          pnlEast.add(tbCharacter[i] = new JToggleButton(scaleNames[i]));
          tbCharacter[i].addActionListener(action);
        }
      }
    }

    taResults.setBackground(new Color(250, 250, 250));
    taResults.setForeground(new Color( 20,  20,  20));
    taResults.setEditable(false);
    this.add(new JScrollPane(taResults,
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), BorderLayout.CENTER);
    this.setPreferredSize(new Dimension(800, 600));
    cbScales.setSelectedIndex(0);
/*
    // 重複しないスケールを列挙
    {
      final ScaleType[] scales = ScaleType.values();
      final boolean[] valid = new boolean[scales.length];
      valid[0] = true;
      for (int i = 1, size = scales.length; i < size; ++i) {
        boolean isValid = true;

        final int thisBits = scales[i].bits();
        FIND: for (int j = 0; j < i; ++j) {
          if (valid[j]) {
            final int otherBits = scales[j].bits();
            for (int k = 0; k < 12; ++k) {
              final int invertBits = ((thisBits << k) & 0xFFF) | (thisBits >> (12-k));
              if (invertBits == otherBits) {
                isValid = false;
                break FIND;
              }
            }
          }
        }
        valid[i] = isValid;
      }

      final StringBuilder sb = new StringBuilder();
      for (int i = 0, size = scales.length; i < size; ++i) {
        if (valid[i]) {
          sb.append(scales[i].name());
          sb.append('\n');
        }
      }

      final JTextArea ta = new JTextArea();
      this.add(new JScrollPane(ta,
          JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
          JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), BorderLayout.EAST);
      ta.setText(sb.toString());
    }
*/
  }

  private int getTonicTone() {
    for (int i = 0, size = rbKey.length; i < size; ++i) {
      if (rbKey[i].isSelected()) {
        return i;
      }
    }
    return -1;
  }

  private int getScaleBits() {
    int bits = 0;
    for (int i = 0, size = tbScale.length; i < size; ++i) {
      if (tbScale[i].isSelected()) {
        bits |= 1 << i;
      }
    }
    return bits;
  }

  private void applyScaleToCharacterTone() {
    for (int i = 0, size = tbScale.length; i < size; ++i) {
      final boolean flag = tbScale[i].isSelected();
      tbCharacter[i].setEnabled(flag);
      if (!flag) {
        tbCharacter[i].setSelected(false);
      }
    }
  }

  private int getCharacterBits() {
    int bits = 0;
    for (int i = 0, size = tbCharacter.length; i < size; ++i) {
      if (tbCharacter[i].isSelected()) {
        bits |= 1 << i;
      }
    }
    return bits;
  }

  // テキストで与えられたコード進行をパースして、ルート音とコードタイプの組に変換する。
  // ルート音は C を 0 とする 0～11 の数値で表現する。
  // コードタイプは、ビットパターンで表現する。
  // テキストの大文字と小文字は区別する。
  private static String parseChords(final String text) {
    final String[] strChords = text.split(" "); // 空白で区切る
    final int[][] chords = new int[strChords.length][];
    for (int i = 0, size = strChords.length; i < size; ++i) {
      chords[i] = parseChord(strChords[i]);
    }
    return toString(chords, strChords);
  }

  private static String toString(final int[][] chords, final String[] strChords) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0, size = strChords.length; i < size; ++i) {
      final int bass      = chords[i][0];
      final int signature = chords[i][1];
      for (int k = 0; k < 12; ++k) {
        sb.append((((signature >> k) & 1) == 1) ? '1' : '_');
      }
      if (bass < 10) {
        sb.append(' ');
      }
      sb.append(bass);
      sb.append(": ");
      sb.append(strChords[i]);
      sb.append('\n');
    }

    return sb.toString();
  }

  // TODO: コード進行のビットパターンが与えられたとき、最適なボイシングを生成する。
  //       なるべく声部の独立性をたもつ。

  // FIXME: UST に対応する。
  private static int[] parseChord(final String text) {
    final int onPosition = text.lastIndexOf('/');
    if (onPosition == 0) {
      // "/" のまえにコードタイプの指定がなかった。
      throw new RuntimeException("Invalid Chord");
    }
    if (onPosition+1 >= text.length()) {
      // "/" のうしろにベース音の指定がなかった。
      throw new RuntimeException("Invalid Chord");
    }

    int bass = -1; // on コードで指定されたベース
    if (onPosition > 0) { // on コード？
      // on コードのベース音を求める。
      switch (text.charAt(onPosition+1)) {
      case 'C': bass =  0; break;
      case 'D': bass =  2; break;
      case 'E': bass =  4; break;
      case 'F': bass =  5; break;
      case 'G': bass =  7; break;
      case 'A': bass =  9; break;
      case 'B': bass = 11; break;
      default: throw new RuntimeException("Invalid Chord"); // 不正な音名が与えられた。
      }
      // 変化記号による影響を反映する。
      //int typePosition = text.length();
      EXIT_FOR: for (int i = onPosition+2, size = text.length(); i < size; ++i) {
        switch (text.charAt(i)) {
        case 'b': bass = flatNote(bass); break;
        case '#': bass = sharpNote(bass); break;
        default: /*typePosition = i;*/ break EXIT_FOR;
        }
      }
    }

    final String name = (bass == -1) ? text : text.substring(0, onPosition);

    int root = -1; // コードタイプで指定されたルート
    switch (text.charAt(0)) {
    case 'C': root =  0; break;
    case 'D': root =  2; break;
    case 'E': root =  4; break;
    case 'F': root =  5; break;
    case 'G': root =  7; break;
    case 'A': root =  9; break;
    case 'B': root = 11; break;
    default: throw new RuntimeException("Invalid Chord"); // 不正な音名が与えられた。
    }

    // 変化記号による影響を反映する。
    int typePosition = text.length();
    EXIT_FOR: for (int i = 1, size = text.length(); i < size; ++i) {
      switch (text.charAt(i)) {
      case 'b': root = flatNote(root); break;
      case '#': root = sharpNote(root); break;
      default: typePosition = i; break EXIT_FOR; // 変化記号を読み終わったのでループを抜ける。
      }
    }

    // コードの種類を求め、0 bit 目が C 音になるように転回する。
    final int signature = shiftUp(parseChordKind(name.substring(typePosition)), root);
    return new int[] {(bass >= 0) ? bass : root, (signature << 12) | (1 << bass)};
  }

  private static int parseChordKind(final String kind) {
    int[] notes = new int[7];
    notes[0] = 1;
    notes[1] = 1<<4;
    notes[2] = 1<<7;
    for (int i = 0, size = kind.length(); i < size;) {
      if (kind.startsWith("sus2", i)) {
        notes[1] = 1<<2; i += 4;
      } else if (kind.startsWith("m", i)) {
        notes[1] = 1<<3; i += 1;
      } else if (kind.startsWith("sus4", i)) {
        notes[1] = 1<<5; i += 4;
      } else if (kind.startsWith("omit3", i)) {
        notes[1] = 0; i += 4;
      } else if (kind.startsWith("b5", i)) {
        notes[2] = 1<<6; i += 2;
      } else if (kind.startsWith("aug", i)) {
        notes[2] = 1<<8; i += 3;
      } else if (kind.startsWith("#5", i)) {
        notes[2] = 1<<8; i += 2;
      } else if (kind.startsWith("omit5", i)) {
        notes[2] = 0; i += 5;
      } else if (kind.startsWith("6", i)) {
        notes[3] = 1<<9; i += 1;
      } else if (kind.startsWith("7", i)) {
        notes[3] = 1<<10; i += 1;
      } else if (kind.startsWith("M7", i)) {
        notes[3] = 1<<11; i += 2;
      } else if (kind.startsWith("dim7", i)) {
        notes[1] = 1<<3; notes[2] = 1<<6; notes[3] = 1<<9; i += 4;
      } else if (kind.startsWith("add9", i)) {
        notes[4] = 1<<2; i += 4;
      } else
          if (kind.startsWith("(", i)
          && !kind.startsWith("(,", i)
          && !kind.startsWith("()", i)
          &&  kind.endsWith(")")
          && !kind.endsWith(",)"))
      {
        i += 1;
        for (;;) {
          if (kind.startsWith("b9", i)) {
            notes[4] = 1<<1; i += 2;
          } else if (kind.startsWith("9", i)) {
            notes[4] = 1<<2; i += 1;
          } else if (kind.startsWith("#9", i)) {
            notes[4] = 1<<3; i += 2;
          } else if (kind.startsWith("11", i)) {
            notes[5] = 1<<5; i += 2;
          } else if (kind.startsWith("#11", i)) {
            notes[5] = 1<<6; i += 3;
          } else if (kind.startsWith("b13", i)) {
            notes[6] = 1<<8; i += 3;
          } else if (kind.startsWith("13", i)) {
            notes[6] = 1<<9; i += 2;
          } else if (kind.startsWith(",", i)) {
            /*            */ i += 1;
          } else if (kind.startsWith(")", i)) {
            /*            */ i += 1;
            break;
          } else {
            throw new RuntimeException("Invalid Chord Signature: \"" + kind + "\"");
          }
        }
      } else {
        // 不正なコードタイプが与えられた。
        throw new RuntimeException("Invalid Chord Signature: \"" + kind + "\"");
      }
    }

    int signature = 0;
    for (final int tone : notes) {
      signature |= tone;
    }
    return signature;
  }

  // 与えられた音を半音上げる。
  private static int sharpNote(final int note) {
    return (note + 1) % 12;
  }

  // 与えられた音を半音上げる。
  private static int flatNote(final int note) {
    return (note + 11) % 12;
  }

  /**
   * 与えられたスケール／コード識別子を上方向に移調する。
   * @param signatrue
   * @param count 12 を法とするものとする。
   * @return
   */
  private static int shiftUp(final int signatrue, final int count) {
    return ((signatrue << count) & 0xFFF) | (signatrue >> (12 - count));
  }

  /**
   * 与えられたスケール／コード識別子を下方向に移調する。
   * @param signatrue
   * @param count 12 を法とするものとする。
   * @return
   */
  //private static int shiftDown(final int signatrue, final int count) {
  //  return ((signatrue << (12-count)) & 0xFFF) | (signatrue >> count);
  //}


  /**
   * @param args
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        JFrame frame = new JFrame("Diatonic Chords Finder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ChordFinder());
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
      }
    });
  }

}
