import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider, theme } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#3b82f6',
          colorInfo: '#3b82f6',
          borderRadius: 8,
          fontFamily: "-apple-system, BlinkMacSystemFont, 'SF Pro Text', 'Segoe UI', Roboto, sans-serif",
          colorBgContainer: '#1e293b',
          colorBgElevated: '#1e293b',
          colorBorderSecondary: '#334155',
          fontSize: 14,
        },
        components: {
          Layout: {
            siderBg: '#0f172a',
            headerBg: '#0f172a',
            bodyBg: '#0f172a',
          },
          Menu: {
            darkItemBg: '#0f172a',
            darkSubMenuItemBg: '#0f172a',
            darkItemSelectedBg: '#1e3a5f',
            darkItemHoverBg: '#1e293b',
          },
          Card: {
            colorBgContainer: '#1e293b',
          },
          Table: {
            colorBgContainer: '#1e293b',
            headerBg: '#0f172a',
          },
          ProTable: {
            colorBgContainer: '#1e293b',
          },
        },
      }}
    >
      <App />
    </ConfigProvider>
  </React.StrictMode>,
);
