import { createStyles } from 'antd-style';

export const useStyles = createStyles(({ token, css }) => ({
  card: css`
    border-radius: ${token.borderRadiusLG}px;
    transition: box-shadow 0.2s, transform 0.2s;
    &:hover {
      box-shadow: ${token.boxShadowSecondary};
      transform: translateY(-1px);
    }
    .ant-statistic-title {
      color: ${token.colorTextSecondary} !important;
      font-size: 13px;
    }
    .ant-statistic-content-value {
      font-size: 28px;
      font-weight: 600;
    }
  `,
  content: css`
    display: flex;
    align-items: center;
    justify-content: space-between;
  `,
  info: css`
    flex: 1;
  `,
  icon: css`
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: ${token.colorBgContainer};
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    color: ${token.colorPrimary};
  `,
}));
