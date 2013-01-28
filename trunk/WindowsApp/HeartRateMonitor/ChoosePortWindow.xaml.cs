using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using System.IO.Ports;
using System.IO;
using BGAPI;
using System.Threading;

namespace HeartRateMonitor
{
    /// <summary>
    /// Логика взаимодействия для ChoosePortWindow.xaml
    /// </summary>
    public partial class ChoosePortWindow : Window
    {
        public bool CloseArgument { get; set; }

        public ChoosePortWindow()
        {
            InitializeComponent();
            string[] ports = SerialPort.GetPortNames();
            portsComboBox.ItemsSource = ports;
        }

        private void OkButtonClick(object sender, RoutedEventArgs e)
        {
            MainWindow.PortName = this.portsComboBox.SelectionBoxItem.ToString();
            this.CloseArgument = true;
            this.Close();
        }

        private void CancelButtonClick(object sender, RoutedEventArgs e)
        {
            this.CloseArgument = false;
            this.Close();
        }
    }
}
