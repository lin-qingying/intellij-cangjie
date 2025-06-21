import React, { useState, useEffect } from 'react';
import { Card, Typography } from 'antd';
import axios from 'axios';

const { Title, Paragraph, Text } = Typography;

const PrivacyPolicy: React.FC = () => {
  const [lastUpdated, setLastUpdated] = useState<string>(new Date().toLocaleDateString());
  const [currentYear, setCurrentYear] = useState<number>(new Date().getFullYear());

  useEffect(() => {
    // 可以从API获取最后更新日期，这里简化处理
    setLastUpdated(new Date().toLocaleDateString('zh-CN'));
    setCurrentYear(new Date().getFullYear());
  }, []);

  return (
    <div className="privacy-policy-container" style={{ maxWidth: '900px', margin: '0 auto', padding: '20px' }}>
      <Card 
        style={{ 
          boxShadow: '0 0 20px rgba(0, 0, 0, 0.05)',
          borderRadius: '8px',
          padding: '10px'
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '30px' }}>
          <Title level={2} style={{ 
            color: '#2c3e50',
            borderBottom: '2px solid #eee',
            paddingBottom: '15px'
          }}>
            Intellij-CangJie 插件隐私政策
          </Title>
          <div style={{ 
            backgroundColor: '#f5f5f5',
            padding: '8px 15px',
            borderRadius: '20px',
            display: 'inline-block',
            marginBottom: '20px',
            fontSize: '0.9em'
          }}>
            最后更新日期: {lastUpdated}
          </div>
        </div>

        <div className="section" style={{ marginBottom: '40px', paddingBottom: '20px', borderBottom: '1px solid #eee' }}>
          <Paragraph>
            感谢您使用本插件。本隐私政策旨在帮助您了解我如何收集、使用和保护您的个人信息。我非常重视您的隐私，并致力于透明地告知您数据处理实践。
          </Paragraph>

          <div style={{ 
            backgroundColor: '#fffde7',
            padding: '15px',
            borderLeft: '4px solid #ffd600',
            margin: '20px 0',
            borderRadius: '4px'
          }}>
            <Paragraph>
              请仔细阅读本隐私政策，以便了解在使用插件时，您的数据将如何被处理。如果您不同意本政策中的任何条款，请关闭遥测开关。
            </Paragraph>
          </div>
        </div>

        <div className="section" style={{ marginBottom: '40px', paddingBottom: '20px', borderBottom: '1px solid #eee' }}>
          <Title level={3} style={{ 
            color: '#3498db',
            marginTop: '40px',
            fontSize: '1.6em',
            borderLeft: '4px solid #3498db',
            paddingLeft: '15px'
          }}>1. 信息收集</Title>
          <Paragraph>我收集以下类型的信息：</Paragraph>
          <ul style={{ marginBottom: '25px', paddingLeft: '25px' }}>
            <li style={{ marginBottom: '12px' }}>
              <Text strong>使用数据</Text>：我收集有关您如何使用插件的遥测数据，包括功能使用频率、操作时间等。这些数据帮助我了解哪些功能最受欢迎，哪些功能可能需要改进。
            </li>
            <li style={{ marginBottom: '12px' }}>
              <Text strong>环境信息</Text>：为了提供更好的服务，我可能会收集您的 IDE 版本、操作系统版本和 Java 版本等技术信息。这些信息帮助我诊断兼容性问题并优化插件性能。
            </li>
            <li style={{ marginBottom: '12px' }}>
              <Text strong>系统标识符</Text>：我使用随机生成的匿名标识符来区分不同的用户，这不会识别您的个人身份。这有助于我了解用户群体规模和使用模式。
            </li>
            <li style={{ marginBottom: '12px' }}>
              <Text strong>错误报告</Text>：当插件遇到错误时，我可能会收集错误日志和相关环境信息，以帮助我诊断和修复问题。
            </li>
          </ul>

          <Paragraph><Text strong>我不收集的信息：</Text></Paragraph>
          <ul style={{ marginBottom: '25px', paddingLeft: '25px' }}>
            <li style={{ marginBottom: '12px' }}>您的个人身份信息（如姓名、电子邮件地址等）</li>
            <li style={{ marginBottom: '12px' }}>您的代码内容或项目结构</li>
            <li style={{ marginBottom: '12px' }}>您的密码或认证凭据</li>
            <li style={{ marginBottom: '12px' }}>您的 IP 地址或精确位置</li>
          </ul>
        </div>

        <div className="section" style={{ marginBottom: '40px', paddingBottom: '20px', borderBottom: '1px solid #eee' }}>
          <Title level={3} style={{ 
            color: '#3498db',
            marginTop: '40px',
            fontSize: '1.6em',
            borderLeft: '4px solid #3498db',
            paddingLeft: '15px'
          }}>2. 信息使用</Title>
          <Paragraph>我使用收集的信息：</Paragraph>
          <ul style={{ marginBottom: '25px', paddingLeft: '25px' }}>
            <li style={{ marginBottom: '12px' }}>改进插件的功能和性能</li>
            <li style={{ marginBottom: '12px' }}>分析用户使用模式，以便做出更好的开发决策</li>
            <li style={{ marginBottom: '12px' }}>诊断和修复问题</li>
            <li style={{ marginBottom: '12px' }}>提供更好的用户体验</li>
            <li style={{ marginBottom: '12px' }}>了解用户需求，规划未来的功能开发</li>
          </ul>

          <Paragraph>所有数据分析均以汇总和匿名的形式进行，不会针对特定个人进行分析。</Paragraph>
        </div>

        <div className="section" style={{ marginBottom: '40px', paddingBottom: '20px', borderBottom: '1px solid #eee' }}>
          <Title level={3} style={{ 
            color: '#3498db',
            marginTop: '40px',
            fontSize: '1.6em',
            borderLeft: '4px solid #3498db',
            paddingLeft: '15px'
          }}>3. 信息共享</Title>
          <Paragraph>
            我不会将您的个人信息出售、交易或转让给第三方。我可能会分享匿名的统计数据，但这些数据不会包含任何可识别个人身份的信息。
          </Paragraph>

          <Paragraph>在以下情况下，我可能会共享某些信息：</Paragraph>
          <ul style={{ marginBottom: '25px', paddingLeft: '25px' }}>
            <li style={{ marginBottom: '12px' }}>为了遵守法律法规的要求</li>
            <li style={{ marginBottom: '12px' }}>为了保护权利、财产或安全</li>
          </ul>
        </div>

        <div className="section" style={{ marginBottom: '40px', paddingBottom: '20px', borderBottom: '1px solid #eee' }}>
          <Title level={3} style={{ 
            color: '#3498db',
            marginTop: '40px',
            fontSize: '1.6em',
            borderLeft: '4px solid #3498db',
            paddingLeft: '15px'
          }}>4. 数据安全</Title>
          <Paragraph>我采取合理的安全措施保护您的信息，防止未经授权的访问、使用或披露。所有收集的遥测数据都经过加密传输。</Paragraph>

          <Paragraph>我的安全措施包括：</Paragraph>
          <ul style={{ marginBottom: '25px', paddingLeft: '25px' }}>
            <li style={{ marginBottom: '12px' }}>使用 HTTPS 加密传输所有数据</li>
            <li style={{ marginBottom: '12px' }}>定期审查和更新安全实践</li>
            <li style={{ marginBottom: '12px' }}>限制对数据的访问，仅授权人员可以访问</li>
            <li style={{ marginBottom: '12px' }}>对收集的数据进行匿名化处理</li>
          </ul>

          <Paragraph>尽管我努力保护您的信息，但请注意，互联网传输方式不能保证100%的安全性。</Paragraph>
        </div>

        <div className="section" style={{ marginBottom: '40px', paddingBottom: '20px', borderBottom: '1px solid #eee' }}>
          <Title level={3} style={{ 
            color: '#3498db',
            marginTop: '40px',
            fontSize: '1.6em',
            borderLeft: '4px solid #3498db',
            paddingLeft: '15px'
          }}>5. 用户选择</Title>
          <Paragraph>您可以随时在插件的设置中选择禁用遥测数据收集。禁用后，将不再收集任何使用数据。</Paragraph>

          <Paragraph>禁用遥测数据收集的步骤：</Paragraph>
          <ol style={{ marginBottom: '25px', paddingLeft: '25px' }}>
            <li style={{ marginBottom: '12px' }}>打开 IntelliJ IDEA</li>
            <li style={{ marginBottom: '12px' }}>进入 Settings/Preferences</li>
            <li style={{ marginBottom: '12px' }}>找到 Tools 部分下的遥测设置(CangJie Telemetry)</li>
            <li style={{ marginBottom: '12px' }}>取消选中"允许收集使用数据"选项</li>
            <li style={{ marginBottom: '12px' }}>点击 Apply 或 OK 保存设置</li>
          </ol>
        </div>

        <div className="section" style={{ marginBottom: '40px', paddingBottom: '20px', borderBottom: '1px solid #eee' }}>
          <Title level={3} style={{ 
            color: '#3498db',
            marginTop: '40px',
            fontSize: '1.6em',
            borderLeft: '4px solid #3498db',
            paddingLeft: '15px'
          }}>6. 数据保留</Title>
          <Paragraph>
            我会在合理必要的时间内保留收集的数据，以实现本隐私政策中描述的目的。一般情况下，遥测数据会保留不超过12个月。当数据不再需要时，我会安全地删除或匿名化处理这些数据。
          </Paragraph>
        </div>

        <div className="section" style={{ marginBottom: '40px', paddingBottom: '20px', borderBottom: '1px solid #eee' }}>
          <Title level={3} style={{ 
            color: '#3498db',
            marginTop: '40px',
            fontSize: '1.6em',
            borderLeft: '4px solid #3498db',
            paddingLeft: '15px'
          }}>7. 政策变更</Title>
          <Paragraph>
            我可能会不时更新本隐私政策。任何变更都会在此页面上发布，重大变更也会通过插件通知您。我鼓励您定期查看本政策，以了解我如何保护您的信息。
          </Paragraph>

          <Paragraph>政策更新后，继续使用插件即表示您同意新的条款。</Paragraph>
        </div>

        <div className="section" style={{ marginBottom: '40px', paddingBottom: '20px', borderBottom: '1px solid #eee' }}>
          <Title level={3} style={{ 
            color: '#3498db',
            marginTop: '40px',
            fontSize: '1.6em',
            borderLeft: '4px solid #3498db',
            paddingLeft: '15px'
          }}>8. 联系我</Title>
          <div style={{ 
            backgroundColor: '#f5f5f5',
            padding: '20px',
            borderRadius: '8px',
            margin: '20px 0'
          }}>
            <Paragraph>如果您对本隐私政策有任何疑问或建议，请通过以下方式联系我：</Paragraph>
            <Paragraph><Text strong>电子邮件</Text>: <a href="mailto:contact@cangnova.cn">contact@cangnova.cn</a></Paragraph>
            <Paragraph><Text strong>Gitee</Text>: <a href="https://gitee.com/Lin_Qing_Ying/intellij-cangjie" target="_blank" rel="noopener noreferrer">https://gitee.com/Lin_Qing_Ying/intellij-cangjie</a></Paragraph>
            <Paragraph><Text strong>GitCode</Text>: <a href="https://gitcode.com/OpenCangjieCommunity/intellij-cangjie" target="_blank" rel="noopener noreferrer">https://gitcode.com/OpenCangjieCommunity/intellij-cangjie</a></Paragraph>
            <Paragraph><Text strong>GitHub</Text>: <a href="https://github.com/lin-qingying/intellij-cangjie" target="_blank" rel="noopener noreferrer">https://github.com/lin-qingying/intellij-cangjie</a></Paragraph>
            
            <a 
              href="https://gitee.com/Lin_Qing_Ying/intellij-cangjie/issues" 
              target="_blank" 
              rel="noopener noreferrer"
              style={{
                display: 'inline-block',
                backgroundColor: '#3498db',
                color: 'white',
                padding: '10px 20px',
                borderRadius: '4px',
                marginTop: '10px',
                transition: 'background-color 0.3s'
              }}
            >
              提交问题或建议
            </a>
          </div>
        </div>

        <div style={{ 
          marginTop: '60px',
          paddingTop: '20px',
          borderTop: '1px solid #eee',
          fontSize: '0.9em',
          color: '#7f8c8d',
          textAlign: 'center'
        }}>
          <Paragraph>&copy; {currentYear} CangNova. 保留所有权利。</Paragraph>
          <Paragraph>本插件是一个开源项目，遵循 Apache 2.0 许可证。</Paragraph>
        </div>
      </Card>
    </div>
  );
};

export default PrivacyPolicy; 